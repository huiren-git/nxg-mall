package com.nxg.mall.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CouponUpdateDTO {
    @NotNull(message = "优惠券ID不能为空")
    private Long id;

    @NotBlank(message = "优惠券标题不能为空")
    private String title;

    @NotNull(message = "优惠券分类不能为空")
    private Integer category; // 1:全场, 2:品类, 3:单品

    @NotNull(message = "优惠方式不能为空")
    private Integer type; // 1:满减, 2:折扣, 3:无门槛

    private BigDecimal conditionAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;

    @NotNull(message = "发放总量不能为空")
    @Min(value = 1, message = "发放总量至少为1")
    private Integer totalCount;

    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    // 适用范围ID列表（如果是品类券则存类目ID，单品券则存商品ID）
    private List<Long> rangeIds;
}