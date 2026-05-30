package com.nxg.mall.analytics.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SalesForecastVO {
    private SummaryMetrics summary;

    // 注释为待实现的日预测 TODO
    /**
    private ChartData chart;
    private List<AiSuggestion> aiSuggestions;
    private List<EvaluationMetric> evaluationMetrics;
     */


    @Data
    public static class SummaryMetrics {
        private BigDecimal accuracy; // 模型准确率
        private BigDecimal expectedRevenue; // 预计营收
        private BigDecimal growthRate; // 增长率
        private BigDecimal seasonalityCoefficient; // 季节性波动系数
    }

    /**
    @Data
    public static class ChartData {
        private List<String> dates;
        private List<Integer> historyData;
        private List<Integer> forecastData;
        private List<Integer> confidenceUpper; // 对应前端置信区间
        private List<Integer> confidenceLower; // 对应前端下界
    }

    @Data
    public static class AiSuggestion {
        private String title;
        private String description;
        private String type; // warning, info, success
    }

    @Data
    public static class EvaluationMetric {
        private String key;
        private String metric;
        private String value;
        private String status;
    }
    */
}
