package com.nxg.mall.api.service.impl;

import com.alibaba.fastjson2.JSON;
import com.nxg.mall.api.mapper.BalanceLogMapper;
import com.nxg.mall.api.mapper.UserMapper;
import com.nxg.mall.api.service.BalanceService;
import com.nxg.mall.common.dto.GenericEventDTO;
import com.nxg.mall.common.entity.UserBalanceLog;
import com.nxg.mall.common.enums.EventType;
import com.nxg.mall.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class BalanceServiceImpl implements BalanceService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BalanceLogMapper balanceLogMapper;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String recharge(Long userId, BigDecimal amount) {
        // 1. 生成唯一充值流水号
        String orderNo = "REC" + System.currentTimeMillis() + userId;

        // 2. 创建流水记录 (初始状态为进行中)
        UserBalanceLog logEntry = new UserBalanceLog();
        logEntry.setUserId(userId);
        logEntry.setOrderNo(orderNo);
        logEntry.setAmount(amount);
        logEntry.setType(1); // 充值
        logEntry.setStatus(1); // 目前直接设为成功，后续接入支付后应设为0
        balanceLogMapper.insert(logEntry);

        // 3. 更新用户账户余额 (增加金额)
        // 注意：生产环境建议使用 update t_user set balance = balance + amount where id = userId
        boolean updated = userMapper.updateBalance(userId, amount);
        if (!updated) {
            throw new BusinessException("充值失败，账户更新异常");
        }

        // 4. 发送 Kafka 埋点消息 (用于财务统计、充值热度分析)
        sendRechargeEvent(userId, orderNo, amount);

        log.info("用户 {} 充值成功，金额：{}，订单号：{}", userId, amount, orderNo);
        return orderNo;
    }

    /**
     * 用户充值余额
     * @param userId
     * @param orderNo
     * @param amount
     */
    private void sendRechargeEvent(Long userId, String orderNo, BigDecimal amount) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("orderNo", orderNo);
            data.put("amount", amount);

            GenericEventDTO<Map<String, Object>> event = new GenericEventDTO<>();
            event.setType(EventType.USER_RECHARGE);
            event.setData(data);
            event.setUserId(userId);
            event.setEventId(UUID.randomUUID().toString());
            event.setTimestamp(System.currentTimeMillis());

            kafkaTemplate.send("nxg_business_event", JSON.toJSONString(event));
        } catch (Exception e) {
            log.error("充值埋点发送失败", e);
        }
    }
}
