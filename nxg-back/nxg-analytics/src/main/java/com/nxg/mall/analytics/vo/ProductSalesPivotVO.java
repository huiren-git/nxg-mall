package com.nxg.mall.analytics.vo;

import com.nxg.mall.analytics.entity.ProductMetricDetail;
import lombok.Data;

import java.util.List;

// ProductSalesPivotVO.java (大包聚合对象)
@Data
public class ProductSalesPivotVO {
    private Long productId;
    private String productName;
    private Integer currentStock;
    private Integer avgDailySales;

    // 对应前端 evaluation 对象
    private EvaluationInfo evaluation;

    // 对应供应安全卡片
    private Integer daysToOutOfStock;
    private Integer restockDeadlineDays;
    private Integer stockUrgencyPercent; // 动态计算的百分比进度条

    // 对应 ECharts 的时序数据
    private List<TrendItem> trends;

    // 对应底部 Table
    private List<ProductMetricDetail> metricsTable;

    @Data
    public static class EvaluationInfo {
        private Integer score;
        private String healthStatus;
        private String riskWarning;
        private String suggestedAction;
    }

    @Data
    public static class TrendItem {
        private String period;
        private Integer sales;
        private Boolean isForecast;
    }
}