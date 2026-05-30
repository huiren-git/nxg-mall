package com.nxg.mall.api.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nxg.mall.common.dto.GenericEventDTO;
import com.nxg.mall.common.dto.payload.OrderMessageDTO;
import com.nxg.mall.common.dto.PayDTO;
import com.nxg.mall.common.dto.PaymentItemDTO;
import com.nxg.mall.common.entity.Order;
import com.nxg.mall.common.entity.OrderItem;
import com.nxg.mall.common.entity.User;
import com.nxg.mall.common.enums.EventType;
import com.nxg.mall.common.exception.BusinessException;
import com.nxg.mall.api.mapper.OrderMapper;
import com.nxg.mall.api.mapper.UserMapper;
import com.nxg.mall.api.service.IOrderItemService;
import com.nxg.mall.api.service.PayService;
import com.nxg.mall.common.utils.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PayServiceImpl implements PayService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private IOrderItemService orderItemService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class) // 保证扣款、订单状态、明细状态更新的原子性
    public void processPay(PayDTO payDTO) {
        // 1. 获取当前用户 ID
        Long userId = SecurityUtils.getUserId();

        // 2. 基础校验：订单是否存在且属于当前用户，状态是否为待支付(0)
        Order order = orderMapper.selectByOrderNo(payDTO.getOrderNo());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在或无权访问");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态异常，无法支付");
        }

        // 3. 金额校验
        // 支付总额累计
        BigDecimal totalInput = payDTO.getPayments().stream()
                .map(PaymentItemDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 注意：这里必须与数据库中的 pay_amount (实付金额) 进行比较，该金额在下单时已扣除优惠
        if (totalInput.compareTo(order.getPayAmount()) != 0) {
            throw new BusinessException("支付金额与订单实付金额不一致");
        }

        // 4. 执行支付扣减逻辑
        // 假设目前 payments 列表中只有余额支付 (type=1)
        PaymentItemDTO balanceItem = payDTO.getPayments().stream()
                .filter(item -> item.getType() == 1)
                .findFirst()
                .orElseThrow(() -> new BusinessException("未找到余额支付信息"));

        // 执行实际的扣款（内部应包含余额充足校验和乐观锁更新）
        handleBalancePay(userId, balanceItem.getAmount());

        // 5. 更新订单主表信息 (t_order)
        order.setStatus(1); // 设置为已支付
        order.setPayTime(LocalDateTime.now());
        order.setPayType(1); // 余额支付
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 6. 核心修改：同步更新订单明细表状态 (t_order_item)
        // 根据 order_no 批量将明细表的 is_paid 改为 1
        LambdaUpdateWrapper<OrderItem> itemUpdateWrapper = new LambdaUpdateWrapper<>();
        itemUpdateWrapper.eq(OrderItem::getOrderNo, order.getOrderNo())
                .set(OrderItem::getIsPaid, 1);
        orderItemService.update(itemUpdateWrapper);

        // 7. (可选) 如果有优惠券，此处可以执行核销逻辑（如果下单时只是预占）
        // updateCouponStatus(order.getCouponIds(), userId);

        // 7. 准备 OrderMessageDTO 数据 (用于 Sales 查看购物日志)
        OrderMessageDTO messageDTO = new OrderMessageDTO();
        BeanUtils.copyProperties(order, messageDTO);

        // 查询明细并转换为 DTO 中的 Detail 格式
        List<OrderItem> orderItems = orderItemService.listByOrderNo(order.getOrderNo());
        List<OrderMessageDTO.OrderItemDetail> details = orderItems.stream().map(item -> {
            OrderMessageDTO.OrderItemDetail detail = new OrderMessageDTO.OrderItemDetail();
            BeanUtils.copyProperties(item, detail);
            // 如果 OrderItem 里只有 categoryId，建议在这里查出 categoryName 补全
            return detail;
        }).collect(Collectors.toList());
        messageDTO.setItems(details);

        // 8. 封装为标准 GenericEventDTO 信封
        GenericEventDTO<OrderMessageDTO> event = GenericEventDTO.<OrderMessageDTO>builder()
                .eventId(UUID.randomUUID().toString())
                .type(EventType.PAYMENT) // 标记为成交事件
                .timestamp(System.currentTimeMillis())
                .userId(userId)
                .data(messageDTO)
                .build();

        // 9. 注册事务同步：确保余额扣减成功后才发出消息
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaTemplate.send("nxg_business_event", JSON.toJSONString(event));
            }
        });
    }

    private void handleBalancePay(Long userId, BigDecimal amount) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("账户余额不足");
        }

        // 数据库层面扣减，防止并发导致金额算错
        // SQL 示例: UPDATE t_user SET balance = balance - #{amount} WHERE id = #{userId}
        int updatedRows = userMapper.reduceBalance(userId, amount);
        if (updatedRows == 0) {
            throw new BusinessException("支付失败，余额可能已发生变动");
        }
    }
}
