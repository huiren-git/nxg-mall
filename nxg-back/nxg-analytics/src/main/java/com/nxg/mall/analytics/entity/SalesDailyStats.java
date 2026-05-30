package com.nxg.mall.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("ads_sales_daily_stats")
public class SalesDailyStats {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private BigDecimal totalSalesAmount;
    private String salesTrend;
    private Integer totalOrderCount;
    private String orderTrend;
    private Integer pendingOrderCount;
    private String pendingTrend;
    private BigDecimal avgOrderValue;
    private String avgValueTrend;
}
