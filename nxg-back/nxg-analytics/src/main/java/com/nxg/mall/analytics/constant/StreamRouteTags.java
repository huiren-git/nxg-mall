package com.nxg.mall.analytics.constant;

import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.util.OutputTag;

/**
 * 侧输出流标签全局配置类
 * 注意：必须带上匿名内部类的 {} 以保留泛型信息，否则 Flink 会报类型擦除错误
 */
public class StreamRouteTags {
    // 流量类标签
    public static final OutputTag<JSONObject> BROWSE_TAG = new OutputTag<JSONObject>("browse-log"){};
    public static final OutputTag<JSONObject> SEARCH_TAG = new OutputTag<JSONObject>("search-log"){};

    // 业务类标签 (为后面的 BusinessEventAnalysisJob 准备)
    public static final OutputTag<JSONObject> TRADE_TAG = new OutputTag<JSONObject>("trade-log"){};
    public static final OutputTag<JSONObject> RECHARGE_TAG = new OutputTag<JSONObject>("recharge-log"){};
    public static final OutputTag<JSONObject> REGISTER_TAG = new OutputTag<JSONObject>("register-log"){};
    public static final OutputTag<JSONObject> LOGIN_TAG = new OutputTag<JSONObject>("login-log"){};

    // 审计类标签
    public static final OutputTag<JSONObject> AUDIT_TAG = new OutputTag<JSONObject>("audit-log"){};

    // 大盘日指标汇总标签
    public static final OutputTag<JSONObject> DAILY_SUMMARY_TAG = new OutputTag<JSONObject>("daily-summary-log"){};

    // 每日交易大盘统计标签
    public static final OutputTag<JSONObject> SALES_DAILY_STATS_TAG = new OutputTag<JSONObject>("sales-daily-stats-output"){};

    // 商品分析大盘标签
    public static final OutputTag<JSONObject> PRODUCT_ANALYSIS_TAG = new OutputTag<JSONObject>("product-analysis-output"){};

    // 用户画像宽表更新标签
    public static final OutputTag<JSONObject> USER_INSIGHT_TAG = new OutputTag<JSONObject>("user-insight-output"){};

    //
    // 用户画像明细流标签
    public static final OutputTag<JSONObject> USER_PORTRAIT_TAG = new OutputTag<JSONObject>("user-portrait-output"){};

    // 商品销量排行更新标签
    public static final OutputTag<JSONObject> PRODUCT_SALES_RANK_TAG = new OutputTag<JSONObject>("product-sales-rank-output"){};

    // 销售预测宽表标签
    public static final OutputTag<JSONObject> ADS_SALES_FORECAST_TAG = new OutputTag<JSONObject>("ads-sales-forecast-output"){};

    // 异常监控记录标签
    public static final OutputTag<JSONObject> ANOMALY_LOG_TAG = new OutputTag<JSONObject>("anomaly-log-output"){};

    // 销售小时趋势标签
    public static final OutputTag<JSONObject> SALES_HOURLY_TREND_TAG = new OutputTag<JSONObject>("sales-hourly-trend-output"){};

    // 系统异常事件队列标签
    public static final OutputTag<JSONObject> SYSTEM_ANOMALY_QUEUE_TAG = new OutputTag<JSONObject>("system-anomaly-queue-output"){};

    // 商品单一销售洞察标签
    public static final OutputTag<JSONObject> PRODUCT_SALES_INSIGHT_TAG = new OutputTag<JSONObject>("product-sales-insight-output"){};

    // 商品指标明细标签
    public static final OutputTag<JSONObject> PRODUCT_METRIC_DETAIL_TAG = new OutputTag<JSONObject>("product-metric-detail-output"){};

    // 商品销量趋势流
    public static final OutputTag<JSONObject> PRODUCT_SALES_TREND_TAG = new OutputTag<JSONObject>("product-sales-trend-side-output") {};

    // =========================================================================
    // 2. DWS/ADS 汇总层 - 结构化指标多维宽表标签 (泛型为具体的强类型实体类)
    // =========================================================================
    // 直接放在这里，不需要单独建新类，它跟上面几辆“车”是并列跑在 Flink 管道里的
    public static final OutputTag<JSONObject> CUSTOMER_STAT_TAG = new OutputTag<JSONObject>("customer-stat-side-output"){};
}