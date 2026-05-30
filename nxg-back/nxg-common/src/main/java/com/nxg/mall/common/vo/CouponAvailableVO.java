package com.nxg.mall.common.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponAvailableVO {
    private Long userCouponId; // 用户持有的券关联ID
    private String title;
    private Integer type; // 1-满减, 2-折扣, 3-无门槛
    private BigDecimal conditionAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;

    private Boolean isUsable; // 是否可用
    private String reason;    // 不可用原因（如：还差 ¥10.00）
}
