package com.nxg.mall.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CouponSaveDTO {
    private String title;
    private Integer category;
    private Integer type;
    private BigDecimal conditionAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private Integer totalCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    // 对应前端选择的品类ID或商品ID列表
    private List<Long> rangeIds;
}
