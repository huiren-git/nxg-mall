package com.nxg.mall.common.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponUserVO {
    // 来自 t_coupon_user
    private Long id;            // 用户优惠券记录ID
    private Integer status;     // 使用状态: 1-未使用, 2-已使用, 3-已过期
    private LocalDateTime createTime; // 领取时间

    // 来自 t_coupon
    private Long couponId;      // 优惠券定义ID
    private String title;       // 标题
    private Integer type;       // 1-满减, 2-折扣
    private BigDecimal conditionAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
