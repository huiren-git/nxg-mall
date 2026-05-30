package com.nxg.mall.analytics.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_daily_summary")
public class DailySummary {
    @TableId
    private LocalDate statDate;
    private Integer uv;
    private Integer pv;
    private Integer newUsers;
    private BigDecimal totalRevenue;
    private Integer orderCount;
    private LocalDateTime updateTime;
    private Integer tradeUsers;
    private Integer rechargeUsers;
    private BigDecimal rechargeAmount;
    private BigDecimal refundAmount;
    private BigDecimal discountAmount;
}
