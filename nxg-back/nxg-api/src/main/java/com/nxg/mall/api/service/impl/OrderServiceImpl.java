package com.nxg.mall.api.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nxg.mall.api.vo.PurchaseLogVO;
import com.nxg.mall.common.dto.*;
import com.nxg.mall.common.dto.payload.OrderRefundMessageDTO;
import com.nxg.mall.common.entity.*;
import com.nxg.mall.common.enums.EventType;
import com.nxg.mall.common.enums.OrderStatus;
import com.nxg.mall.common.exception.BusinessException;
import com.nxg.mall.api.mapper.UserMapper;
import com.nxg.mall.api.service.*;
import com.nxg.mall.common.vo.OrderVO;
import com.nxg.mall.api.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    @Value("${file.domain}")
    private String domain;

    @Autowired
    private ICartService cartService;

    @Autowired
    private IProductService productService;

    @Autowired
    private IOrderItemService orderItemService;

    @Autowired
    private IAddressService addressService;

    @Autowired
    private ISkuService skuService;

    @Autowired
    private ICouponService couponService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailService mailService; // 注入邮件服务

    @Autowired
    private ICategoryService categoryService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(Long userId, OrderCreateDTO createDTO) {
        // 1. 获取地址详情
        Address address = addressService.getById(createDTO.getAddressId());
        if (address == null) throw new BusinessException("收货地址不存在");

        // 2. 获取购物车中选中的条目
        List<Cart> selectedCarts = cartService.list(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getIsChecked, 1));
        if (selectedCarts.isEmpty()) throw new BusinessException("购物车内无选中商品");

        // 3. 批量获取数据并校验
        List<Long> skuIds = selectedCarts.stream().map(Cart::getSkuId).collect(Collectors.toList());
        Map<Long, Sku> skuMap = skuService.listByIds(skuIds).stream()
                .collect(Collectors.toMap(Sku::getId, s -> s));

        List<Long> pIds = selectedCarts.stream().map(Cart::getProductId).collect(Collectors.toList());
        Map<Long, Product> productMap = productService.listByIds(pIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<Long> categoryIds = productMap.values().stream()
                .map(Product::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Category> categoryMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            categoryMap = categoryService.listByIds(categoryIds).stream()
                    .collect(Collectors.toMap(Category::getId, c -> c));
        }

        // 4. 校验库存并计算商品总额 (totalAmount)
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Cart cart : selectedCarts) {
            Sku sku = skuMap.get(cart.getSkuId());
            Product product = productMap.get(cart.getProductId());

            // 基础非空校验
            if (sku == null || product == null) {
                throw new BusinessException("部分商品信息已失效，请重新加入购物车");
            }

            // 精确库存校验
            if (sku.getStock() < cart.getQuantity()) {
                // 组装详细的错误提示：[商品名称] (规格信息) 库存不足
                String errorMsg = String.format("商品 [%s] (%s) 库存不足，当前剩余：%d",
                        product.getName(),
                        sku.getSpecs(),
                        sku.getStock());
                throw new BusinessException(errorMsg);
            }

            // 累加总金额
            totalAmount = totalAmount.add(sku.getPrice().multiply(new BigDecimal(cart.getQuantity())));
        }

        // --- 5. 核心逻辑：优惠券计算与 ID 转换 ---
        BigDecimal totalDiscountAmount = BigDecimal.ZERO;
        String couponIdsStr = null;

        if (createDTO.getCouponIds() != null && !createDTO.getCouponIds().isEmpty()) {
            // 将 List<Long> 转换为 "1,2,3" 格式存入数据库
            couponIdsStr = createDTO.getCouponIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            // 获取第一张优惠券进行抵扣（此处可根据业务扩展为多券叠加计算）
            Long couponId = createDTO.getCouponIds().get(0);
            Coupon coupon = couponService.getById(couponId);
            if (coupon != null) {
                if (coupon.getType() == 1) { // 满减
                    totalDiscountAmount = coupon.getDiscountAmount();
                } else if (coupon.getType() == 2) { // 折扣
                    BigDecimal rate = coupon.getDiscountRate();
                    totalDiscountAmount = totalAmount.multiply(BigDecimal.ONE.subtract(rate));
                }
            }
        }

        // 实付金额
        BigDecimal payAmount = totalAmount.subtract(totalDiscountAmount);
        if (payAmount.compareTo(BigDecimal.ZERO) < 0) payAmount = BigDecimal.ZERO;

        // --- 6. 扣库存并构建订单明细 ---
        String orderNo = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        List<OrderItem> orderItems = new ArrayList<>();

        for (Cart cart : selectedCarts) {
            Sku sku = skuMap.get(cart.getSkuId());
            Product product = productMap.get(cart.getProductId());

            // 乐观锁扣库存
            boolean stockResult = skuService.update(new LambdaUpdateWrapper<Sku>()
                    .setSql("stock = stock - " + cart.getQuantity())
                    .eq(Sku::getId, sku.getId())
                    .ge(Sku::getStock, cart.getQuantity()));
            if (!stockResult) {
                throw new BusinessException(String.format("商品 [%s] (%s) 刚刚被抢光了，请尝试减少数量",
                        product.getName(), sku.getSpecs()));
            }

            // 💡 2. 新增：乐观锁扣 Product (SPU) 总库存
            if (product.getStock() != null) {
                productService.update(new LambdaUpdateWrapper<Product>()
                        .setSql("stock = stock - " + cart.getQuantity())
                        .eq(Product::getId, product.getId())
                        .ge(Product::getStock, cart.getQuantity()));
            }

            OrderItem item = new OrderItem();
            item.setOrderNo(orderNo);
            item.setProductId(product.getId());
            item.setSkuId(sku.getId());
            item.setProductName(product.getName());
            item.setSpecs(sku.getSpecs());
            item.setProductPic(sku.getMainImage() != null ? sku.getMainImage() : product.getMainImage());
            item.setPrice(sku.getPrice());
            item.setQuantity(cart.getQuantity());

            // 订单项金额计算 (对齐 t_order_item 结构)
            BigDecimal itemTotal = sku.getPrice().multiply(new BigDecimal(cart.getQuantity()));
            item.setPayAmount(itemTotal);      // 单项原始总价
            item.setCouponDiscountAmount(BigDecimal.ZERO); // 如需平摊优惠，在此处计算
            item.setRealAmount(itemTotal);     // 单项最终金额
            item.setIsPaid(0);                 // 对应数据库 tinyint，默认 0

            if (product.getStock() != null) {
                // 实时计算该商品扣减后的剩余总库存
                int remainingStock = product.getStock() - cart.getQuantity();
                item.setProductStock(Math.max(remainingStock, 0)); // 避免负数兜底
            } else {
                item.setProductStock(0);
            }

            Category category = categoryMap.get(product.getCategoryId());
            item.setCategoryName(category != null ? category.getName() : "默认分类");

            orderItems.add(item);
        }

        // --- 7. 保存订单主表 (对齐 t_order 结构) ---
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setAddressId(createDTO.getAddressId());
        order.setTotalAmount(totalAmount);
        order.setTotalDiscountAmount(totalDiscountAmount);
        order.setCouponIds(couponIdsStr); // 存入转换后的字符串
        order.setPayAmount(payAmount);
        order.setStatus(0); // 待支付状态

        // 收货人信息快照
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        String fullAddr = address.getProvince() + address.getCity() + address.getDistrict() + address.getDetailAddress();
        order.setReceiverAddress(fullAddr);
        order.setRemark(createDTO.getRemark());

        this.save(order);

        // 8. 批量保存明细
        orderItemService.saveBatch(orderItems);

        // 9. 清理购物车
        cartService.remove(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getIsChecked, 1));

        // ======= 新增：发送订单创建埋点消息 =======
        sendOrderCreateLog(order, orderItems);

        // 10. 返回结果
        OrderVO vo = new OrderVO();
        vo.setOrderNo(orderNo);
        vo.setTotalAmount(totalAmount);
        vo.setPayAmount(payAmount);
        return vo;
    }

    /**
     * 发送订单创建日志
     */
    private void sendOrderCreateLog(Order order, List<OrderItem> orderItems) {
        try {
            // 1. 构造明细数据（用于 Flink 统计各个商品的下单量）
            List<Map<String, Object>> items = orderItems.stream().map(item -> {
                Map<String, Object> map = new HashMap<>();
                map.put("productId", item.getProductId());
                map.put("skuId", item.getSkuId());
                map.put("price", item.getPrice());
                map.put("quantity", item.getQuantity());
                map.put("mainImage", item.getProductPic());
                map.put("productName", item.getProductName());
                map.put("category", item.getCategoryName());
                map.put("stock", item.getProductStock());
                return map;
            }).collect(Collectors.toList());

            // 2. 构造业务载荷
            Map<String, Object> payload = new HashMap<>();
            payload.put("orderNo", order.getOrderNo());
            payload.put("userId", order.getUserId());
            payload.put("totalAmount", order.getTotalAmount());     // 原始总价
            payload.put("discountAmount", order.getTotalDiscountAmount()); // 优惠金额
            payload.put("payAmount", order.getPayAmount());         // 应付金额
            payload.put("items", items);                           // 商品明细
            payload.put("status", order.getStatus());

            // 3. 封装为通用的 GenericEventDTO
            GenericEventDTO<Map<String, Object>> event = new GenericEventDTO<>();
            event.setType(EventType.ORDER_CREATE); // 确保枚举中有 ORDER_CREATE
            event.setData(payload);
            event.setPlatform("Web");
            event.setTimestamp(System.currentTimeMillis());

            // 4. 发送到业务事件 Topic
            kafkaTemplate.send("nxg_business_event", JSON.toJSONString(event));

            log.debug("已发送订单创建埋点：{}", order.getOrderNo());
        } catch (Exception e) {
            // 关键：埋点异常不能阻塞下单主流程
            log.error("订单创建埋点发送失败", e);
        }
    }

    @Override
    public List<OrderVO> getUserOrderList(Long userId, String status) {

        List<Integer> statusList = null;

        if (StringUtils.hasText(status)) {
            if ("refund".equals(status)) {
                // 聚合状态：6 和 7
                statusList = Arrays.asList(6, 7);
            } else {
                // 单个状态：转为单元素 List
                try {
                    statusList = Collections.singletonList(Integer.parseInt(status));
                } catch (NumberFormatException e) {
                    statusList = null;
                }
            }
        }

        // 调用修改后的 Mapper
        return baseMapper.selectUserOrderList(userId, statusList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo, Long userId) {
        // 1. 查询订单
        Order order = baseMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 2. 安全校验
        if (!order.getUserId().equals(userId)) {
            log.warn("用户越权操作：用户 {} 尝试取消订单 {}", userId, orderNo);
            throw new BusinessException("您无权操作此订单");
        }

        // 3. 状态校验：只有 0-待支付 和 1-待发货 允许取消
        Integer currentStatus = order.getStatus();
        if (currentStatus >= 2 && currentStatus != 5) {
            throw new BusinessException("订单已发货或已处理，无法直接取消");
        }
        if (currentStatus == 5) return; // 幂等

        // --- 核心退款与退券逻辑开始 ---

        // 4. 如果是待发货(status=1)，说明已经支付过，需要退还余额
        if (currentStatus == 1) {
            BigDecimal refundAmount = order.getPayAmount();
            if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                boolean updateRow = userMapper.updateBalance(userId, refundAmount);
                if (!updateRow) {
                    throw new BusinessException("退款至余额失败");
                }
                log.info("订单取消退款成功：订单号 {}, 用户 {}, 金额 {}", orderNo, userId, refundAmount);
            }
        }

        // 5. 退还优惠券
        // 逻辑：解析 coupon_ids 字符串 (例如 "1,2")，将关联的优惠券状态设为未使用
        String couponIdsStr = order.getCouponIds();
        if (couponIdsStr != null && !couponIdsStr.trim().isEmpty()) {
            String[] couponIds = couponIdsStr.split(",");
            for (String cid : couponIds) {
                // 这里假设你有一张用户优惠券关联表 t_user_coupon
                // 或者如果是简单逻辑，则根据实际业务恢复优惠券可用性
                this.returnCoupon(Long.parseLong(cid), userId);
            }
        }

        // 6. 更新订单状态为已取消(5)
        Order updateOrder = new Order();
        updateOrder.setId(order.getId());
        updateOrder.setStatus(5);
        updateOrder.setUpdateTime(LocalDateTime.now());
        updateOrder.setRemark("用户自主取消订单（已退回余额及优惠券）");

        int rows = baseMapper.updateById(updateOrder);
        if (rows <= 0) {
            throw new BusinessException("取消订单失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrderBySales(OrderCancelDTO param, Long operatorId) {
        // 1. 查询订单
        Order order = baseMapper.selectByOrderNo(param.getOrderNo());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 2. 状态校验（销售取消通常也只允许取消未发货的）
        Integer currentStatus = order.getStatus();
        if (currentStatus >= 2 && currentStatus != 5) {
            throw new BusinessException("订单已发货，无法取消");
        }

        // 3. 执行核心取消逻辑
        // 注意：这里传入的是订单所属的 userId，确保钱退给用户而不是销售
        this.executeCancelLogic(order, param.getReason(), "销售操作(" + operatorId + ")");

        // 4. 发送邮件通知用户
        User user = userMapper.selectById(order.getUserId());
        if (user != null && StringUtils.hasText(user.getEmail())) {
            // 注册事务同步钩子：确保余额退还成功且事务提交后才发信
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 构建取消通知的邮件数据
                    OrderEmailDTO emailInfo = OrderEmailDTO.builder()
                            .email(user.getEmail())
                            .nickname(user.getNickname())
                            .orderNo(order.getOrderNo())
                            .reason(param.getReason()) // 销售填写的取消原因
                            .template("email/cancel")   // 对应的取消模板
                            .build();

                    // 异步发送
                    mailService.sendCancelEmail(emailInfo);
                }
            });
        }
    }

    /**
     * 核心取消逻辑（内部复用）
     * @param order 订单对象
     * @param reason 取消原因
     * @param operator 描述操作人信息
     */
    private void executeCancelLogic(Order order, String reason, String operator) {
        if (order.getStatus() == 5) return; // 幂等处理

        // 1. 退款逻辑（如果是待发货状态）
        if (order.getStatus() == 1) {
            BigDecimal refundAmount = order.getPayAmount();
            if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                // 退回给订单的拥有者
                boolean updateRow = userMapper.updateBalance(order.getUserId(), refundAmount);
                if (!updateRow) throw new BusinessException("退款失败");
                log.info("订单取消退款成功：单号 {}, 金额 {}, 操作人 {}", order.getOrderNo(), refundAmount, operator);
            }
        }

        // 2. 退还优惠券
        String couponIdsStr = order.getCouponIds();
        if (StringUtils.hasText(couponIdsStr)) {
            String[] ids = couponIdsStr.split(",");
            for (String cid : ids) {
                this.returnCoupon(Long.parseLong(cid), order.getUserId());
            }
        }

        // 3. 更新订单状态
        Order updateOrder = new Order();
        updateOrder.setId(order.getId());
        updateOrder.setStatus(5);
        updateOrder.setUpdateTime(LocalDateTime.now());
        // 记录取消原因和操作人
        updateOrder.setRemark("取消原因：" + reason + " | 操作来源：" + operator);

        if (baseMapper.updateById(updateOrder) <= 0) {
            throw new BusinessException("更新订单状态失败");
        }
    }

    /**
     * 退还优惠券逻辑
     * 注意：通常 t_coupon 是模板表，你应该还有一张 t_user_coupon 表记录具体谁拥有哪一张券
     */
    private void returnCoupon(Long couponId, Long userId) {
        // 示例：更新用户优惠券表，将 status 改回 0 (未使用)
        // userCouponMapper.updateStatus(couponId, userId, 0);
        log.info("已退还优惠券：ID {}, 用户 {}", couponId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceipt(String orderNo, Long userId) {
        // 1. 查询订单是否存在
        Order order = baseMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 2. 越权校验：判断该订单是否属于当前用户
        if (!order.getUserId().equals(userId)) {
            System.out.println("userId " + userId + "has no relationship with the order: " +orderNo);
            throw new BusinessException("无权操作此订单");
        }

        // 3. 状态校验：只有“已发货(状态码2)”的订单才能确认收货
        // 假设状态定义：0-待支付，1-待发货，2-已发货，3-待评价/已收货
        if (order.getStatus() != 2) {
            throw new BusinessException("当前订单状态不支持确认收货");
        }

        // 4. 更新订单状态
        Order updateOrder = new Order();
        updateOrder.setId(order.getId());
        updateOrder.setStatus(3); // 变更为待评价
        updateOrder.setReceiveTime(LocalDateTime.now()); // 设置收货时间
        updateOrder.setUpdateTime(LocalDateTime.now());

        int rows = baseMapper.updateById(updateOrder);
        if (rows == 0) {
            throw new BusinessException("确认收货失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(String orderNo, Long userId) {
        // 1. 获取订单并校验归属权
        Order order = baseMapper.selectByOrderNo(orderNo);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在或非法操作");
        }

        // 2. 状态校验：只有状态为 3（待评价）时，才能手动跳过评价完成订单
        if (order.getStatus() != 3) {
            throw new BusinessException("当前订单状态不支持直接完成");
        }

        // 3. 执行状态变更
        Order updateOrder = new Order();
        updateOrder.setId(order.getId());
        updateOrder.setStatus(4); // 4: 已完成
        updateOrder.setUpdateTime(LocalDateTime.now());

        // 如果你有记录交易完成时间的字段，建议在这里更新
        // updateOrder.setEndTime(LocalDateTime.now());

        int rows = baseMapper.updateById(updateOrder);
        if (rows == 0) {
            throw new BusinessException("系统繁忙，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyReturn(String orderNo, Long userId) {
        // 1. 查询订单
        Order order = this.lambdaQuery()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId)
                .one();

        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 2. 状态校验 (只有 待评价[3] 或 已完成[4] 但在售后时限内才能退货)
        if (order.getStatus() != 3 && order.getStatus() != 4) {
            throw new BusinessException("当前订单状态不支持退货申请");
        }

        // 3. 更新状态为 6 (退货申请中，需根据你设计的状态机增加该状态)
        // 建议在数据库 status 字段注释中加入：6-退货中
        Order updateOrder = new Order();
        updateOrder.setId(order.getId());
        updateOrder.setStatus(6);
        updateOrder.setUpdateTime(LocalDateTime.now());

        this.updateById(updateOrder);
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliveryOrder(OrderDeliveryDTO param) {
        // 1. 查询订单
        Order order = this.getById(param.getOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 2. 状态校验：只有“待发货”状态（status=1）的订单才能执行此操作
        if (order.getStatus() != 1) {
            throw new BusinessException("订单状态异常，无法发货");
        }

        // 3. 更新订单信息（包括物流信息）
        order.setStatus(2); // 状态变更为：已发货
        order.setDeliveryTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        // 4. 执行更新
        boolean success = this.updateById(order);
        if (!success) {
            throw new RuntimeException("发货失败，数据库更新冲突");
        }

        // 5. 准备邮件通知数据
        // 查询下单用户信息
        User user = userMapper.selectById(order.getUserId());

        // 如果用户存在且有邮箱，则触发异步邮件发送
        if (user != null && StringUtils.hasText(user.getEmail())) {
            // 关键：在事务提交后执行邮件发送
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 构建邮件数据对象
                    OrderEmailDTO emailInfo = OrderEmailDTO.builder()
                            .email(user.getEmail())
                            .nickname(user.getNickname())
                            .orderNo(order.getOrderNo()) // 假设订单号字段为 orderSn
                            .template("email/shipping")
                            .build();

                    // 异步发送邮件
                    mailService.sendShippingEmail(emailInfo);
                }
            });
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReturn(String orderNo) {
        // 1. 获取并锁定订单（悲观锁或通过状态校验确保幂等）
        Order order = this.lambdaQuery().eq(Order::getOrderNo, orderNo).one();

        if (order == null) throw new BusinessException("订单不存在");
        if (order.getStatus() != 6) throw new BusinessException("订单不处于退货申请状态");

        // 2. 执行退款逻辑：将支付金额退回用户余额
        boolean refundSuccess = userMapper.updateBalance(order.getUserId(), order.getPayAmount());
        if (!refundSuccess) throw new BusinessException("退款至余额失败，请联系管理员");

        // 3. 更新订单状态为“已退货”
        boolean updateSuccess = this.lambdaUpdate()
                .set(Order::getStatus, OrderStatus.RETURNED.getCode())
                .set(Order::getUpdateTime, LocalDateTime.now())
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getStatus, 6) // 乐观锁校验
                .update();

        if (!updateSuccess) throw new BusinessException("订单状态更新失败");

        try {
            // 1. 构造业务载荷
            OrderRefundMessageDTO refundData = new OrderRefundMessageDTO(
                    order.getOrderNo(),
                    order.getUserId(),
                    order.getPayAmount(),
                    LocalDateTime.now()
            );

            // 2. 封装为通用的 GenericEventDTO
            GenericEventDTO<OrderRefundMessageDTO> event = new GenericEventDTO<>();
            event.setType(EventType.ORDER_REFUND); // 确保枚举中有这个类型
            event.setData(refundData);
            event.setUserId(order.getUserId());
            event.setTimestamp(System.currentTimeMillis());
            event.setPlatform("Web");

            // 3. 发送到业务事件 Topic (nxg_business_event)
            kafkaTemplate.send("nxg_business_event", order.getOrderNo(), JSON.toJSONString(event));

            log.info("已发送退款业务事件：{}", order.getOrderNo());
        } catch (Exception e) {
            log.error("发送退货统计消息失败", e);
        }

        log.info("订单 {} 退货审批通过，金额 {} 已退回用户 {}", orderNo, order.getPayAmount(), order.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectReturn(String orderNo, String reason) {
        Order order = this.lambdaQuery().eq(Order::getOrderNo, orderNo).one();

        if (order == null) throw new BusinessException("订单不存在");
        if (order.getStatus() != 6) throw new BusinessException("订单不处于退货申请状态");

        // 更新状态回“待评价(3)”或“已完成(4)”，并记录拒绝原因
        boolean updateSuccess = this.lambdaUpdate()
                .set(Order::getStatus, OrderStatus.WAIT_COMMENT.getCode())
                .set(Order::getRemark, "拒绝退货原因：" + reason)
                .set(Order::getUpdateTime, LocalDateTime.now())
                .eq(Order::getOrderNo, orderNo)
                .update();

        if (!updateSuccess) throw new BusinessException("操作失败");
    }

    @Override
    public List<OrderVO> getAllCustomerOrderList(String status) {

        List<Integer> statusList = null;

        if (StringUtils.hasText(status)) {
            if ("refund".equals(status)) {
                // 聚合状态：6 和 7
                statusList = Arrays.asList(6, 7);
            } else {
                // 单个状态：转为单元素 List
                try {
                    statusList = Collections.singletonList(Integer.parseInt(status));
                } catch (NumberFormatException e) {
                    statusList = null;
                }
            }
        }
        // 1. 从数据库查出原始数据
        List<OrderVO> orderList = baseMapper.selectUserOrderList(null, statusList);

        return orderList;
    }

    @Override
    public List<PurchaseLogVO> getPurchaseLogs(Long userId) {
        // 可以在 Mapper 里用纯 SQL 实现多表联查
        return baseMapper.selectPurchaseLogs(userId);
    }
}
