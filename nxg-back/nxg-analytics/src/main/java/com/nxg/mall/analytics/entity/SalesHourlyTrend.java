package com.nxg.mall.analytics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("ads_sales_hourly_trend")
public class SalesHourlyTrend {
    private Long id;
    private LocalDate statDate;
    private String hourNode;
    private BigDecimal salesAmount;
    private Integer orderCount;
}
