package com.nxg.mall.analytics.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class UserInsightVO {
    // 核心指标
    private Integer totalActiveUsers;
    private Integer corePaidUsers;
    private BigDecimal avgCustomerValue;
    private BigDecimal repurchaseRate;

    // 图表数据
    private List<Map<String, Object>> geoDistribution;    // 地理分布
    private Map<String, Integer> consumptionPreference;    // 消费偏好 (雷达图)
    private List<PowerRatio> purchasePower;                // 购买力分布 (饼图)

    @Data
    @AllArgsConstructor
    public static class PowerRatio {
        private String name;
        private BigDecimal value;
    }
}