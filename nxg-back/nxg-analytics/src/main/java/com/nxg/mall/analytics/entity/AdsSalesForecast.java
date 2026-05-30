package com.nxg.mall.analytics.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ads_sales_forecast")
public class AdsSalesForecast {
    @TableId
    private String forecastMonth;
    private String modelName;
    private BigDecimal expectedRevenue;
    private BigDecimal expectedGrowthRate;
    private BigDecimal seasonalityCoefficient;
    private LocalDateTime createTime;
}
