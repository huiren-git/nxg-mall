package com.nxg.mall.analytics.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class SalesTradeDashboardVO {
    // 顶部四个指标卡片数据
    private List<StatCard> stats;

    // 图表数据
    private List<ChartNode> chartData;

    // 底部需关注订单 (复用之前的 anomaly_queue 逻辑)
    private List<Map<String, Object>> riskOrders;

    @Data
    @AllArgsConstructor
    public static class StatCard {
        private String title;
        private String value;
        private String trend;
        private String color; // #3f8600(升) 或 #cf1322(降)
    }

    @Data
    public static class ChartNode {
        private String name; // 对应 hour_node
        private BigDecimal sales;
    }
}
