package com.nxg.mall.common.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponVO {
    private Long id;
    private String title;
    private Integer category;
    private Integer type;
    private BigDecimal conditionAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private Integer totalCount;
    private Integer takeCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private LocalDateTime createTime;

    /**
     * 当前用户是否已领取 (0:未领取, 1:已领取)
     * 这个字段不是来自 t_coupon 表，而是根据关联查询计算出来的
     */
    private Integer isTaken;
    // 优惠券是否已经抢光
    private Boolean isStockOut;
}