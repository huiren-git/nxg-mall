package com.nxg.mall.analytics.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSummaryVO {
    private Long visitors;       // UV
    private Long pageViews;      // PV
    private Long newUsers;       // 新增
    private Long tradeUsers;     // 成交
    private Long rechargeUsers;  // 充值
}
