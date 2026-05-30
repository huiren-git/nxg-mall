package com.nxg.mall.analytics.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("ads_global_user_summary")
public class AdsGlobalUserSummary {
    @TableId
    private LocalDate statDate;
    private Integer totalActiveUsers;
    private Integer corePaidUsers;
    private BigDecimal globalRepurchaseRate;
    private BigDecimal avgCustomerValue;
    private BigDecimal highConsumptionRatio;
    private LocalDateTime updateTime;
}