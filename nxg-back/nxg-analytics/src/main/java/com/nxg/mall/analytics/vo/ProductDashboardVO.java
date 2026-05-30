package com.nxg.mall.analytics.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ProductDashboardVO {
    // 顶部核心指标
    private BigDecimal totalInventoryValue;
    private BigDecimal avgTurnoverRate;
    private Integer alertSkuCount;

    // 饼图：类别销售贡献
    private List<Map<String, Object>> categorySales;

    // 柱状图：订单状态分布
    private Map<String, List<?>> orderStatusDist;
}
