package com.nxg.mall.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// 1. ProductSalesInsight.java
@Data
@TableName("t_product_sales_insight")
public class ProductSalesInsight {
    @TableId
    private Long productId;
    private String productName;
    private Integer currentStock;
    private Integer avgDailySales;
    private Integer evaluationScore;
    private String healthStatus;
    private String riskWarning;
    private String suggestedAction;
    private Integer daysToOutOfStock;
    private Integer restockDeadlineDays;
    private LocalDateTime updateTime;
}
