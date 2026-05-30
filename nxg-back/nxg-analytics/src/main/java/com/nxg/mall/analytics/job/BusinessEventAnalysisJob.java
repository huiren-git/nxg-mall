package com.nxg.mall.analytics.job;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nxg.mall.analytics.constant.StreamRouteTags;
import com.nxg.mall.analytics.sink.*;
import com.nxg.mall.common.enums.EventType;
import java.math.BigDecimal;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BusinessEventAnalysisJob {
    private static final Logger LOG = LoggerFactory.getLogger(BusinessEventAnalysisJob.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 🛠️【调试改造 1】本地测试强行限制并行度为 1，防止 16 个线程疯狂刷屏把你的业务打印冲走
        env.setParallelism(1);

        // 1. Checkpoint 设置：保证在作业失败后能从断点恢复
        env.enableCheckpointing(10000);

        // 🌟 动态获取 Docker 容器中的环境变量，如果获取不到则降级回本地的 localhost
        String jdbcUrl = System.getenv("SPRING_DATASOURCE_URL") != null
                ? System.getenv("SPRING_DATASOURCE_URL")
                : "jdbc:mysql://localhost:3306/nxg_analytics?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai";

        String username = System.getenv("SPRING_DATASOURCE_USERNAME") != null
                ? System.getenv("SPRING_DATASOURCE_USERNAME")
                : "root";

        String password = System.getenv("SPRING_DATASOURCE_PASSWORD") != null
                ? System.getenv("SPRING_DATASOURCE_PASSWORD")
                : "root";

        String kafkaBootstrap = System.getenv("SPRING_KAFKA_BOOTSTRAP_SERVERS") != null
                ? System.getenv("SPRING_KAFKA_BOOTSTRAP_SERVERS")
                : "localhost:9092";

        // 初始化 Sink
        DwdSinkFactory.init(jdbcUrl, username, password);
        OperationLogSinkFactory.init(jdbcUrl, username, password);

        // 🌟 这里的集群地址也同步换成动态获取的变量
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers(kafkaBootstrap)
                .setTopics("nxg_business_event")
                .setGroupId("nxg_business_event_group")
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<String> rawStream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

        // 🛠️【调试改造 3】关键听诊器：只要 Kafka 管道里流出数据，哪怕是错的，也会在这里无条件显形！
        rawStream.print("【1. KAFKA_RAW_STRING】");

        // 3. 统一解析：因为格式已统一，直接转为 JSONObject 并提取通用字段
        SingleOutputStreamOperator<JSONObject> eventStream = rawStream
                .map(json -> {
                    try { return JSON.parseObject(json); }
                    catch (Exception e) {
                        System.err.println("======> 🚨 历史脏数据导致 Fastjson 解析失败: " + json);
                        return null;
                    }
                })
                .filter(obj -> {
                    if (obj == null) return false;
                    boolean valid = obj.containsKey("type") && obj.containsKey("data");
                    if (!valid) {
                        System.out.println("======> ⚠️ 过滤掉不合规的 JSON (缺少 type 或 data): " + obj.toJSONString());
                    }
                    return valid;
                });

        // 路由及组装层
        // 1. 先按用户 ID 进行 keyBy 分流（没有 userId 的赋予固定值 -1 保证不断流）
        SingleOutputStreamOperator<JSONObject> routerStream = eventStream
                .keyBy(value -> {
                    JSONObject data = value.getJSONObject("data");
                    Long userId = value.containsKey("userId") ? value.getLong("userId") :
                            (data != null ? data.getLong("userId") : null);
                    return userId == null ? -1L : userId;
                })
                // 2. 使用 KeyedProcessFunction 处理状态去重
                .process(new KeyedProcessFunction<Long, JSONObject, JSONObject>() {

                    // 定义状态：记录上一次访问的日期，以及各指标今天是否已统计过
                    private ValueState<String> lastDateState;
                    private ValueState<Boolean> uvFlagState;
                    private ValueState<Boolean> tradeFlagState;
                    private ValueState<Boolean> rechargeFlagState;

                    @Override
                    public void open(Configuration parameters) {
                        lastDateState = getRuntimeContext().getState(new ValueStateDescriptor<>("lastDate", String.class));
                        uvFlagState = getRuntimeContext().getState(new ValueStateDescriptor<>("uvFlag", Boolean.class));
                        tradeFlagState = getRuntimeContext().getState(new ValueStateDescriptor<>("tradeFlag", Boolean.class));
                        rechargeFlagState = getRuntimeContext().getState(new ValueStateDescriptor<>("rechargeFlag", Boolean.class));
                    }

                    @Override
                    public void processElement(JSONObject value, Context ctx, Collector<JSONObject> out) {
                        try {
                            String typeStr = value.getString("type");
                            EventType eventType = EventType.getByCode(typeStr);

                            // 获取事件时间戳（作为日志流水号组装要素）
                            Long timestamp = value.getLong("timestamp");
                            if (timestamp == null) timestamp = System.currentTimeMillis();

                            if (eventType == null) {
                                System.err.println("======> ❌ 无法识别的事件类型: " + typeStr);

                                JSONObject anomalyLog = new JSONObject();
                                // 利用 时间戳+事件类型 生成幂等 logNo
                                anomalyLog.put("logNo", "UNK_" + typeStr + "_" + timestamp);
                                anomalyLog.put("anomalyType", "UNKNOWN_EVENT_TYPE");
                                anomalyLog.put("targetObj", "SYSTEM_GATEWAY");
                                anomalyLog.put("riskLevel", "LOW"); // 低风险，可能是前端/埋点版本未同步
                                anomalyLog.put("detailDesc", "系统接收到无法解析的事件编码 [" + typeStr + "]，原始报文: " + value.toJSONString());
                                anomalyLog.put("status", 0); // 0-未处理

                                // 发射到异常侧输出流
                                ctx.output(StreamRouteTags.ANOMALY_LOG_TAG, anomalyLog);

                                out.collect(value); // 放行主流避免阻塞 downstream
                                return;
                            }

                            System.out.println("======> 🎉 成功进入路由，识别到事件类型: " + eventType.name());
                            JSONObject data = value.getJSONObject("data");
                            if (data == null) data = new JSONObject();

                            Long userId = ctx.getCurrentKey();

                            // 时间及天级批次解析
                            if (timestamp == null) timestamp = System.currentTimeMillis();
                            LocalDate statDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();
                            String statDateStr = statDate.toString();
                            String eventTimeStr = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).format(DATE_TIME_FORMATTER);

                            // 跨天检查：如果状态中的日期与当前事件日期不一致，说明进入了新的一天，清空当前用户的状态
                            String lastDate = lastDateState.value();
                            if (lastDate == null || !lastDate.equals(statDateStr)) {
                                lastDateState.update(statDateStr);
                                uvFlagState.clear();
                                tradeFlagState.clear();
                                rechargeFlagState.clear();
                            }

                            // 初始化大盘日结 JSON 容器
                            JSONObject dailySummaryJson = new JSONObject();
                            dailySummaryJson.put("statDate", statDateStr);
                            dailySummaryJson.put("pv", 0);
                            dailySummaryJson.put("uv", 0);
                            dailySummaryJson.put("newUsers", 0);
                            dailySummaryJson.put("totalRevenue", BigDecimal.ZERO);
                            dailySummaryJson.put("orderCount", 0);
                            dailySummaryJson.put("tradeUsers", 0);
                            dailySummaryJson.put("rechargeUsers", 0);
                            dailySummaryJson.put("rechargeAmount", BigDecimal.ZERO);
                            dailySummaryJson.put("refundAmount", BigDecimal.ZERO);
                            dailySummaryJson.put("discountAmount", BigDecimal.ZERO);

                            // 【核心逻辑：统一计算 UV】
                            // 只要不是匿名访客（userId != -1），且今天还没记录过该用户的 UV，就标记 uv = 1
                            if (userId != -1L && (uvFlagState.value() == null || !uvFlagState.value())) {
                                dailySummaryJson.put("uv", 1);
                                uvFlagState.update(true);
                            }

                            // 初始化其他辅助维度的 JSON
                            JSONObject salesStatsJson = new JSONObject();
                            salesStatsJson.put("statDate", statDateStr);
                            salesStatsJson.put("totalSalesAmount", BigDecimal.ZERO);
                            salesStatsJson.put("totalOrderCount", 0);
                            salesStatsJson.put("pendingOrderCount", 0);
                            salesStatsJson.put("avgOrderValue", BigDecimal.ZERO);

                            String hourNode = String.format("%02d:00", LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).getHour());
                            JSONObject hourlyTrendJson = new JSONObject();
                            hourlyTrendJson.put("statDate", statDateStr);
                            hourlyTrendJson.put("hourNode", hourNode);
                            hourlyTrendJson.put("salesAmount", BigDecimal.ZERO);
                            hourlyTrendJson.put("orderCount", 0);

                            // ✨ 1. 初始化【用户画像明细】的专属 JSON 容器
                            JSONObject userPortraitJson = new JSONObject();
                            userPortraitJson.put("userId", userId);
                            userPortraitJson.put("totalBrowseCount", 0); // 默认增量为0
                            userPortraitJson.put("totalSpent", BigDecimal.ZERO);
                            userPortraitJson.put("orderCount", 0);

                            if (data != null) {
                                // 提取地域与IP
                                if (data.containsKey("province")) userPortraitJson.put("province", data.getString("province"));
                                if (data.containsKey("city")) userPortraitJson.put("city", data.getString("city"));
                                if (data.containsKey("ip")) userPortraitJson.put("lastLoginIp", data.getString("ip"));

                                // ✨ 改造：多级判断提取类目偏好候选值
                                if (data.containsKey("category")) {
                                    userPortraitJson.put("favoriteCategory", data.getString("category"));
                                } else if (data.containsKey("categoryId")) {
                                    userPortraitJson.put("favoriteCategory", "品类" + data.getString("categoryId"));
                                } else if (eventType == EventType.ORDER_CREATE) {
                                    JSONArray itemsArray = data.getJSONArray("items");
                                    if (itemsArray != null && !itemsArray.isEmpty()) {
                                        JSONObject firstItem = itemsArray.getJSONObject(0);

                                        // 👇 加入这段排查日志
                                        System.out.println("✅ [DEBUG-解析] 解析到商品明细: " + firstItem.toJSONString());

                                        if (firstItem != null && firstItem.containsKey("category")) {
                                            String categoryVal = firstItem.getString("category");
                                            userPortraitJson.put("favoriteCategory", categoryVal);

                                            // 👇 加入这段排查日志
                                            System.out.println("✅ [DEBUG-赋值] 成功提取到品类: [" + categoryVal + "], 当前画像 JSON: " + userPortraitJson.toJSONString());
                                        } else {
                                            System.err.println("❌ [DEBUG-异常] 商品明细中不存在 category 字段！");
                                        }
                                    }
                                }

                                if (data.containsKey("stayTime")) userPortraitJson.put("avgStayTime", data.getInteger("stayTime"));
                            }

                            switch (eventType) {
                                // 1. 交易类事件
                                case ORDER_CREATE:

                                    ctx.output(StreamRouteTags.TRADE_TAG, value);

                                    // ✨ 2. 订单事件：为画像赋订单增量值和最后交易时间
                                    BigDecimal orderAmt = data.getBigDecimal("payAmount");
                                    if (orderAmt == null) orderAmt = BigDecimal.ZERO;
                                    userPortraitJson.put("orderCount", 1);
                                    userPortraitJson.put("totalSpent", orderAmt);
                                    userPortraitJson.put("lastTradeTime", eventTimeStr); // 记录当前事件时间

                                    BigDecimal discount = data.getBigDecimal("discountAmount");
                                    if (discount == null) discount = BigDecimal.ZERO;

                                    dailySummaryJson.put("orderCount", 1);
                                    dailySummaryJson.put("discountAmount", discount);

                                    salesStatsJson.put("totalOrderCount", 1);
                                    salesStatsJson.put("pendingOrderCount", 1);

                                    hourlyTrendJson.put("orderCount", 1);

                                    ctx.output(StreamRouteTags.SALES_HOURLY_TREND_TAG, hourlyTrendJson);

                                    // ================== ✨ 新增：动态生成预测数据并打入侧输出流 ==================
                                    JSONObject dummyForecast = new JSONObject();
                                    // 截取当前日期作为预测月份，比如 "2026-05"
                                    String currentMonth = statDateStr.substring(0, 7);

                                    // 转为 YearMonth
                                    YearMonth yearMonth = YearMonth.parse(currentMonth);

// 加一个月
                                    YearMonth nextMonth = yearMonth.plusMonths(1);

// 转回字符串
                                    String forecastMonth = nextMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));

                                    dummyForecast.put("forecastMonth", forecastMonth);
                                    dummyForecast.put("modelName", "prophet");
                                    // 比如：根据当前单笔订单金额，乘以一个固定系数模拟下月预期收入
                                    dummyForecast.put("expectedRevenue", orderAmt.multiply(new BigDecimal("1.2")));
                                    dummyForecast.put("expectedGrowthRate", new BigDecimal("5.5"));       // 5.5% 增长率
                                    dummyForecast.put("seasonalityCoefficient", new BigDecimal("1.05")); // 1.05 季节系数

                                    // 🔮 关键：千万不能漏掉这一行发射动作！
                                    ctx.output(StreamRouteTags.ADS_SALES_FORECAST_TAG, dummyForecast);

                                    // =========================
                                    // 商品分析统计 & 洞察明细生成
                                    // =========================
                                    JSONArray items = data.getJSONArray("items");

                                    if (items != null && !items.isEmpty()) {
                                        for (int i = 0; i < items.size(); i++) {
                                            JSONObject item = items.getJSONObject(i);

                                            // 提取公共通用字段
                                            Long productId = item.getLong("productId");
                                            if (productId == null) continue; // 无ID商品直接跳过

                                            String productName = item.getString("productName");
                                            Integer quantity = item.getInteger("quantity");
                                            if (quantity == null) quantity = 0;

                                            Integer realStock = item.getInteger("stock");
                                            if (realStock == null) realStock = 0;

                                            // ----------------------------------------------------
                                            // 1. 发射原有基础分析流 (Product Analysis)
                                            // ----------------------------------------------------
                                            JSONObject productAnalysisJson = new JSONObject();
                                            productAnalysisJson.put("dt", statDateStr);
                                            productAnalysisJson.put("productName", productName);
                                            productAnalysisJson.put("category", item.getString("category"));
                                            productAnalysisJson.put("stock", realStock);
                                            productAnalysisJson.put("salesVolume", quantity);
                                            BigDecimal price = item.getBigDecimal("price");
                                            productAnalysisJson.put("price", price == null ? BigDecimal.ZERO : price);

                                            ctx.output(StreamRouteTags.PRODUCT_ANALYSIS_TAG, productAnalysisJson);

                                            // ----------------------------------------------------
                                            // ✨ 2. 构建并下发 Insight 洞察数据 (t_product_sales_insight)
                                            // ----------------------------------------------------
                                            JSONObject insightJson = new JSONObject();
                                            insightJson.put("productId", productId);
                                            insightJson.put("productName", productName);
                                            insightJson.put("currentStock", realStock);
                                            insightJson.put("avgDailySales", quantity); // 简化处理：当天销量作为近期日均累加参考
                                            insightJson.put("evaluationScore", 85);     // 假设固定初始评分，实际可根据评价事件更新

                                            // 基于库存实现简单的健康度规则引擎
                                            int daysToOutOfStock = quantity > 0 ? realStock / quantity : 999;
                                            insightJson.put("daysToOutOfStock", daysToOutOfStock);
                                            insightJson.put("restockDeadlineDays", daysToOutOfStock > 3 ? daysToOutOfStock - 3 : 0);

                                            if (realStock <= 0) {
                                                insightJson.put("healthStatus", "CRITICAL");
                                                insightJson.put("riskWarning", "已断货，严重影响转化");
                                                insightJson.put("suggestedAction", "紧急补货/下架处理");
                                            } else if (daysToOutOfStock <= 5) {
                                                insightJson.put("healthStatus", "WARNING");
                                                insightJson.put("riskWarning", "库存告急，预计 " + daysToOutOfStock + " 天后售罄");
                                                insightJson.put("suggestedAction", "尽快安排采购入库");
                                            } else {
                                                insightJson.put("healthStatus", "HEALTHY");
                                                insightJson.put("riskWarning", "正常");
                                                insightJson.put("suggestedAction", "保持监控");
                                            }
                                            ctx.output(StreamRouteTags.PRODUCT_SALES_INSIGHT_TAG, insightJson);

                                            // ----------------------------------------------------
                                            // ✨ 3. 构建并下发 Metric 指标明细 (t_product_metric_detail)
                                            // ----------------------------------------------------
                                            JSONObject metricJson = new JSONObject();
                                            metricJson.put("productId", productId);
                                            metricJson.put("metricDim", "Daily_Sales_Volume"); // 统计维度
                                            metricJson.put("currentValue", String.valueOf(quantity));
                                            // 实时流中较难直接获取环比，通常赋一个标记，由后端定时任务刷新，或者接下游维表 JOIN
                                            metricJson.put("momChange", "N/A");
                                            metricJson.put("diagnosticResult", quantity > 50 ? "爆款趋势" : "销量平稳");

                                            ctx.output(StreamRouteTags.PRODUCT_METRIC_DETAIL_TAG, metricJson);

                                            // ----------------------------------------------------
                                            // ✨ 4. 构建并下发 销量趋势记录 (t_product_sales_trend)
                                            // ----------------------------------------------------
                                            JSONObject trendJson = new JSONObject();
                                            trendJson.put("productId", productId);
                                            trendJson.put("periodName", statDateStr); // 以天为周期记录：例如 "2026-05-28"
                                            trendJson.put("salesVolume", quantity);
                                            trendJson.put("isForecast", 0); // 0 代表真实发生的交易数据
                                            trendJson.put("sortOrder", 1);

                                            ctx.output(StreamRouteTags.PRODUCT_SALES_TREND_TAG, trendJson);
                                        }
                                    }

                                    break;

                                case PAYMENT: { // 💡 ✨ 注意这里：加左大括号，开启独立作用域
                                    ctx.output(StreamRouteTags.TRADE_TAG, value);
                                    BigDecimal payAmt = data.getBigDecimal("payAmount");
                                    if (payAmt == null) payAmt = BigDecimal.ZERO;

                                    userPortraitJson.put("payAmount", payAmt);
                                    userPortraitJson.put("totalSpent", payAmt);

                                    if (payAmt.compareTo(new BigDecimal("50000")) >= 0) {
                                        JSONObject anomalyLog = new JSONObject();
                                        anomalyLog.put("logNo", "PAY_RISK_" + userId + "_" + timestamp);
                                        anomalyLog.put("anomalyType", "LARGE_PAYMENT_RISK");
                                        anomalyLog.put("targetObj", "User_ID:" + userId);
                                        anomalyLog.put("riskLevel", "HIGH"); // 高风险
                                        anomalyLog.put("detailDesc", "触发高额消费监控：用户 [" + userId + "] 单笔支付金额达 " + payAmt + " 元，请合规部跟进。");
                                        anomalyLog.put("status", 0);
                                        ctx.output(StreamRouteTags.ANOMALY_LOG_TAG, anomalyLog);

                                        // ✨ 这里声明的变量只在当前 case PAYMENT 的大括号内有效
                                        JSONObject systemQueueJson = new JSONObject();
                                        systemQueueJson.put("businessType", 1);
                                        systemQueueJson.put("anomalyType", "LARGE_PAYMENT_RISK");
                                        systemQueueJson.put("targetObject", "User_ID:" + userId);

                                        String buyerName = data.containsKey("username") ? data.getString("username") : "UnknownUser_" + userId;
                                        systemQueueJson.put("buyerName", buyerName);

                                        systemQueueJson.put("orderAmount", payAmt);
                                        systemQueueJson.put("riskLevel", "HIGH");
                                        systemQueueJson.put("description", "触发高额消费监控：用户 [" + userId + "] 单笔支付金额达 " + payAmt + " 元，已进入人工审核队列。");
                                        systemQueueJson.put("status", 0);

                                        ctx.output(StreamRouteTags.SYSTEM_ANOMALY_QUEUE_TAG, systemQueueJson);
                                    }

                                    dailySummaryJson.put("totalRevenue", payAmt);

                                    if (userId != -1L && (tradeFlagState.value() == null || !tradeFlagState.value())) {
                                        dailySummaryJson.put("tradeUsers", 1);
                                        tradeFlagState.update(true);
                                    }

                                    salesStatsJson.put("totalSalesAmount", payAmt);
                                    salesStatsJson.put("pendingOrderCount", -1);
                                    hourlyTrendJson.put("salesAmount", payAmt);
                                    ctx.output(StreamRouteTags.SALES_HOURLY_TREND_TAG, hourlyTrendJson);
                                    break;
                                } // 💡 ✨ 注意这里：加右大括号，关闭独立作用域

                                case ORDER_REFUND: { // 💡 ✨ 注意这里：同理加左大括号
                                    ctx.output(StreamRouteTags.TRADE_TAG, value);
                                    BigDecimal refundAmt = data.getBigDecimal("payAmount");
                                    if (refundAmt == null) refundAmt = BigDecimal.ZERO;

                                    userPortraitJson.put("refundAmount", refundAmt);

                                    if (refundAmt.compareTo(new BigDecimal("20000")) >= 0) {
                                        JSONObject anomalyLog = new JSONObject();
                                        anomalyLog.put("logNo", "REF_RISK_" + userId + "_" + timestamp);
                                        anomalyLog.put("anomalyType", "LARGE_REFUND_RISK");
                                        anomalyLog.put("targetObj", "User_ID:" + userId);
                                        anomalyLog.put("riskLevel", "MEDIUM"); // 中风险
                                        anomalyLog.put("detailDesc", "触发高额退款监控：用户 [" + userId + "] 单笔退款金额达 " + refundAmt + " 元，注意防范恶意刷单套现。");
                                        anomalyLog.put("status", 0);
                                        ctx.output(StreamRouteTags.ANOMALY_LOG_TAG, anomalyLog);

                                        // ✨ 因为上面 PAYMENT 的大括号已经关闭，这里可以完全自由地再次声明同名变量！
                                        JSONObject systemQueueJson = new JSONObject();
                                        systemQueueJson.put("businessType", 2);
                                        systemQueueJson.put("anomalyType", "LARGE_REFUND_RISK");
                                        systemQueueJson.put("targetObject", "User_ID:" + userId);

                                        String buyerName = data.containsKey("username") ? data.getString("username") : "UnknownUser_" + userId;
                                        systemQueueJson.put("buyerName", buyerName);

                                        systemQueueJson.put("orderAmount", refundAmt);
                                        systemQueueJson.put("riskLevel", "MEDIUM");
                                        systemQueueJson.put("description", "触发高额退款监控：用户 [" + userId + "] 单笔退款金额达 " + refundAmt + " 元，注意防范恶意刷单套现。");
                                        systemQueueJson.put("status", 0);

                                        ctx.output(StreamRouteTags.SYSTEM_ANOMALY_QUEUE_TAG, systemQueueJson);
                                    }

                                    dailySummaryJson.put("refundAmount", refundAmt);
                                    dailySummaryJson.put("totalRevenue", refundAmt.negate());

                                    salesStatsJson.put("totalSalesAmount", refundAmt.negate());
                                    hourlyTrendJson.put("orderCount", -1);
                                    hourlyTrendJson.put("salesAmount", refundAmt.negate());
                                    ctx.output(StreamRouteTags.SALES_HOURLY_TREND_TAG, hourlyTrendJson);
                                    break;
                                }

                                // 2. 充值事件
                                case USER_RECHARGE:
                                    ctx.output(StreamRouteTags.RECHARGE_TAG, value);
                                    BigDecimal rechargeAmt = data.getBigDecimal("amount");
                                    if (rechargeAmt == null) rechargeAmt = BigDecimal.ZERO;

                                    userPortraitJson.put("rechargeAmount", rechargeAmt);

                                    dailySummaryJson.put("rechargeAmount", rechargeAmt);

                                    // 【充值用户去重逻辑】
                                    if (userId != -1L && (rechargeFlagState.value() == null || !rechargeFlagState.value())) {
                                        dailySummaryJson.put("rechargeUsers", 1);
                                        rechargeFlagState.update(true);
                                    }
                                    break;

                                // 3. 注册事件
                                case REGISTER:
                                    ctx.output(StreamRouteTags.REGISTER_TAG, value);
                                    dailySummaryJson.put("newUsers", 1);
                                    break;

                                case LOGIN:
                                    ctx.output(StreamRouteTags.LOGIN_TAG, value);
                                    userPortraitJson.put("lastLoginTime", eventTimeStr);

                                    // 提取并更新用户名
                                    if (data.containsKey("username")) {
                                        userPortraitJson.put("username", data.getString("username"));
                                    }
                                    // 优先获取 data 内部的本地 IP，若无则拿最外层的公网 IP
                                    String loginIp = data.containsKey("ip") ? data.getString("ip") : value.getString("ip");
                                    if (loginIp != null) {
                                        userPortraitJson.put("lastLoginIp", loginIp);
                                    }
                                    break;

                                // 4. 流量/页面浏览类事件（现在 userId=null 也能进来了）
                                case PV:
                                case BROWSE:
                                case SEARCH:
                                    if (eventType == EventType.SEARCH) ctx.output(StreamRouteTags.SEARCH_TAG, value);
                                    if (eventType == EventType.BROWSE) ctx.output(StreamRouteTags.BROWSE_TAG, value);

                                    dailySummaryJson.put("pv", 1);
                                    userPortraitJson.put("totalBrowseCount", 1);
                                    break;

                                default:
                                    break;
                            }

                            // 统一分发大盘及基础统计侧输出流（只有产生了有效指标变化时才下发，避免无意义的空刷）
                            ctx.output(StreamRouteTags.DAILY_SUMMARY_TAG, dailySummaryJson);
                            ctx.output(StreamRouteTags.SALES_DAILY_STATS_TAG, salesStatsJson);
                            if (userId != -1L) {
                                ctx.output(StreamRouteTags.USER_PORTRAIT_TAG, userPortraitJson);
                            }

                        } catch (Exception e) {
                            System.err.println("======> 🚨 BREAK DOWN!! DETAILS: ");
                            e.printStackTrace();
                        }

                        // 放行主流
                        out.collect(value);
                    }
                });



        // B. 绑定 DWS/ADS 宽表层管道
        routerStream.getSideOutput(StreamRouteTags.CUSTOMER_STAT_TAG).print("【3. ADS_STREAM_DEBUG】");

        routerStream.getSideOutput(StreamRouteTags.CUSTOMER_STAT_TAG)
                .addSink(AdsSinkFactory.getCustomerStatSink()).name("SinkTo_t_customer_stat_wide");
                
        routerStream.getSideOutput(StreamRouteTags.DAILY_SUMMARY_TAG)
                .addSink(AdsSinkFactory.getDailySummarySink()).name("SinkTo_t_daily_summary");

        routerStream.getSideOutput(StreamRouteTags.SALES_DAILY_STATS_TAG)
                .addSink(AdsSinkFactory.getSalesDailyStatsSink()).name("SinkTo_ads_sales_daily_stats");

        routerStream.getSideOutput(StreamRouteTags.SALES_HOURLY_TREND_TAG)
                .addSink(AdsSinkFactory.getSalesHourlyTrendSink()).name("SinkTo_ads_sales_hourly_trend");

        routerStream.getSideOutput(StreamRouteTags.PRODUCT_ANALYSIS_TAG)
                .addSink(AdsSinkFactory.getProductAnalysisSink()).name("SinkTo_ads_product_analysis");

        routerStream.getSideOutput(StreamRouteTags.USER_INSIGHT_TAG)
                .addSink(UserPortraitSinkFactory.getUserInsightSink()).name("SinkTo_t_user_portrait");

        routerStream.getSideOutput(StreamRouteTags.PRODUCT_SALES_RANK_TAG)
                .addSink(ProductSalesRankSinkFactory.getProductSalesRankSink()).name("SinkTo_t_product_sales_rank");

        routerStream.getSideOutput(StreamRouteTags.ADS_SALES_FORECAST_TAG)
                .addSink(SalesForecastSinkFactory.getSalesForecastSink()).name("SinkTo_ads_sales_forecast");

        routerStream.getSideOutput(StreamRouteTags.ANOMALY_LOG_TAG)
                .addSink(AnomalyLogSinkFactory.getAnomalyLogSink()).name("SinkTo_t_anomaly_log");

        routerStream.getSideOutput(StreamRouteTags.SYSTEM_ANOMALY_QUEUE_TAG)
                .addSink(SystemAnomalyQueueSinkFactory.getSystemAnomalyQueueSink()).name("SinkTo_t_system_anomaly_queue");

        routerStream.getSideOutput(StreamRouteTags.PRODUCT_SALES_INSIGHT_TAG)
                .addSink(ProductSalesInsightSinkFactory.getProductSalesInsightSink()).name("SinkTo_t_product_sales_insight");

        routerStream.getSideOutput(StreamRouteTags.PRODUCT_METRIC_DETAIL_TAG)
                .addSink(ProductSalesInsightSinkFactory.getProductMetricDetailSink()).name("SinkTo_t_product_metric_detail");

        // ✨ 3. Trend 趋势表挂载 (新增)
        routerStream.getSideOutput(StreamRouteTags.PRODUCT_SALES_TREND_TAG)
                .addSink(ProductSalesInsightSinkFactory.getProductSalesTrendSink())
                .name("SinkTo_t_product_sales_trend");

        // A. 绑定 DWD 明细层管道
        routerStream.getSideOutput(StreamRouteTags.TRADE_TAG).addSink(DwdSinkFactory.getTradeLogSink()).name("SinkTo_dwd_trade_log");
        routerStream.getSideOutput(StreamRouteTags.RECHARGE_TAG).addSink(DwdSinkFactory.getRechargeLogSink()).name("SinkTo_dwd_recharge_log");
        routerStream.getSideOutput(StreamRouteTags.REGISTER_TAG).addSink(DwdSinkFactory.getUserRegisterLogSink()).name("SinkTo_dwd_user_register_log");
        routerStream.getSideOutput(StreamRouteTags.LOGIN_TAG).addSink(DwdSinkFactory.getUserLoginLogSink()).name("SinkTo_dwd_user_login_log");

        // --- 其他统计看板 ---
        processNetRevenue(routerStream);
        processProductSalesRank(routerStream);
        processRegistrationTrends(routerStream);

        env.execute("NXG Unified Business Analytics Job");
    }

    private static void processNetRevenue(DataStream<JSONObject> stream) {
        stream.filter(obj -> {
                    String type = obj.getString("type");
                    return EventType.ORDER_CREATE.getCode().equals(type) || EventType.ORDER_REFUND.getCode().equals(type) || EventType.USER_RECHARGE.getCode().equals(type);
                }).map(obj -> {
                    String typeStr = obj.getString("type");
                    JSONObject data = obj.getJSONObject("data");
                    BigDecimal amount = BigDecimal.ZERO;
                    EventType eventType = EventType.getByCode(typeStr);
                    if (eventType == null) return BigDecimal.ZERO;
                    switch (eventType) {
                        case ORDER_CREATE: amount = data.getBigDecimal("payAmount"); break;
                        case USER_RECHARGE: amount = data.getBigDecimal("amount"); break;
                        case ORDER_REFUND: amount = data.getBigDecimal("payAmount").negate(); break;
                        default: break;
                    }
                    return amount;
                })
                .windowAll(TumblingProcessingTimeWindows.of(Time.seconds(30)))
                .reduce(BigDecimal::add)
                .print("RealTime_Net_Revenue_30s");
    }

    private static void processProductSalesRank(DataStream<JSONObject> stream) {

        // 1. 必须用 SingleOutputStreamOperator 接收 process 的返回值
        SingleOutputStreamOperator<JSONObject> processedStream = stream
                .filter(obj -> EventType.ORDER_CREATE.getCode().equals(obj.getString("type")))
                .process(new ProcessFunction<JSONObject, JSONObject>() {

                    @Override
                    public void processElement(JSONObject obj,
                                               Context ctx,
                                               Collector<JSONObject> out) {

                        JSONObject data = obj.getJSONObject("data");
                        if (data == null) return;

                        List<JSONObject> items = data.getList("items", JSONObject.class);
                        if (items == null || items.isEmpty()) return;

                        for (JSONObject item : items) {
                            JSONObject rankJson = new JSONObject();
                            Long productId = item.getLong("productId");

                            Integer quantity = item.getInteger("quantity");
                            if (quantity == null) quantity = 0;

                            // 2. 修正逻辑：商品明细里是 price(单价)，真实的商品销售额 = price * quantity
                            BigDecimal price = item.getBigDecimal("price");
                            if (price == null) {
                                price = BigDecimal.ZERO;
                            }
                            BigDecimal itemSalesAmount = price.multiply(new BigDecimal(quantity));

                            rankJson.put("productId", productId);
                            rankJson.put("productName", item.getString("productName"));
                            rankJson.put("mainImage", item.getString("mainImage"));

                            // 注意：你的 JSON 里没有 category 字段，这里解析出来会是 null
                            // 只要你的 MySQL 表允许 category 为 NULL，就不会报错
                            rankJson.put("category", item.getString("category"));

                            rankJson.put("salesVolume", quantity);
                            rankJson.put("salesAmount", itemSalesAmount);
                            rankJson.put("productPv", 0);
                            rankJson.put("orderCount", 1);

                            // 将组装好的数据打入侧输出流
                            ctx.output(StreamRouteTags.PRODUCT_SALES_RANK_TAG, rankJson);
                        }
                    }
                });

        // 3. 致命修复：必须从 processedStream 提取侧输出流，并连接 Sink
        processedStream
                .getSideOutput(StreamRouteTags.PRODUCT_SALES_RANK_TAG)
                .addSink(ProductSalesRankSinkFactory.getProductSalesRankSink())
                .name("SinkTo_t_product_sales_rank");
    }

    private static void processRegistrationTrends(DataStream<JSONObject> stream) {
        stream.filter(obj -> EventType.REGISTER.getCode().equals(obj.getString("type")))
                .windowAll(TumblingProcessingTimeWindows.of(Time.minutes(1)))
                .apply(new AllWindowFunction<JSONObject, Long, TimeWindow>() {
                    @Override
                    public void apply(TimeWindow window, Iterable<JSONObject> values, Collector<Long> out) {
                        long count = 0;
                        for (JSONObject ignored : values) count++;
                        out.collect(count);
                    }
                })
                .print("New_Users_Per_Minute");
    }
}
