package com.nxg.mall.analytics.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TradeStatisticsVO {
    private SummaryData summaryData;
    private ChartData chartData;

    @Data
    public static class SummaryData {
        private BigDecimal totalRevenue;
        private BigDecimal goodsAmount;
        private BigDecimal rechargeAmount;
        private BigDecimal payRate; // 支付转化率(%)
        private BigDecimal refundAmount;
        private BigDecimal discountAmount;
    }

    @Data
    public static class ChartData {
        private List<String> chartDays;
        private List<BigDecimal> revenueSeries;
        private List<BigDecimal> goodsSeries;
        private List<BigDecimal> conversionSeries;
        private List<BigDecimal> refundSeries;
        private List<BigDecimal> discountSeries;
    }
}
