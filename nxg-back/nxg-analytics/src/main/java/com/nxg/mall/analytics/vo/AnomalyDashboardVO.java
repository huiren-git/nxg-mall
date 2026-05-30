package com.nxg.mall.analytics.vo;

import lombok.Data;

import java.util.List;

@Data
public class AnomalyDashboardVO {
    private Integer totalToday;
    private Integer interceptedCount;
    private Integer currentQps;
    private Integer avgResponseTime;
    private Integer healthScore;

    // 图表数据
    private List<String> xAxisData;
    private List<Integer> actualData;
    private List<Integer> baselineData;
}