package com.nxg.mall.api.service.bo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserCouponDetailBO {
    // 1. 来自 t_coupon_user 的信息
    private Long userCouponId; // 用户领取的优惠券记录唯一ID
    private Integer userCouponStatus; // 用户这张券的状态 (1-未使用)

    // 2. 来自 t_coupon 的基本信息
    private Long id; // 优惠券模板ID
    private String title;
    private Integer type; // 1-满减, 2-折扣
    private BigDecimal conditionAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // 3. 来自 t_coupon_scope 的聚合信息 (核心差异点)
    private Integer category; // 范围类型：1-全场, 2-分类, 3-商品
    private List<Long> scopeTargetIds; // 重点：将多行 scope 记录中的 target_id 聚合为列表
}
