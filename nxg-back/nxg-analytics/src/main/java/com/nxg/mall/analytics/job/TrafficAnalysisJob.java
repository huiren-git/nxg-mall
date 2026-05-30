package com.nxg.mall.analytics.job;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.nxg.mall.analytics.constant.StreamRouteTags;
import com.nxg.mall.analytics.sink.AdsSinkFactory;
import com.nxg.mall.analytics.sink.DwdSinkFactory;
import com.nxg.mall.analytics.sink.OperationLogSinkFactory;
import com.nxg.mall.common.enums.EventType;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple5;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class TrafficAnalysisJob {

    public static void main(String[] args) throws Exception {
        ParameterTool params = ParameterTool.fromArgs(args);

        String dbUrl = params.get("db.url", "jdbc:mysql://localhost:3306/nxg_analytics?useSSL=false&serverTimezone=Asia/Shanghai");
        String dbUser = params.get("db.user", "root");
        String dbPass = params.get("db.pass", "root");

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.getConfig().setGlobalJobParameters(params);

        DwdSinkFactory.init(dbUrl, dbUser, dbPass);
        env.enableCheckpointing(5000);

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
                .setTopics("nxg_traffic_log")
                .setGroupId("nxg_traffic_analysis_group")
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<String> rawStream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

        rawStream.print("====== [1. Kafka Raw Output] ======");

        SingleOutputStreamOperator<JSONObject> eventStream = rawStream
                .map(JSON::parseObject)
                .filter(obj -> {
                    if (obj == null) {
                        System.out.println("❌ [2. Filter] 丢弃：JSON 解析为 null");
                        return false;
                    }
                    boolean hasType = obj.containsKey("type");
                    boolean hasData = obj.containsKey("data");
                    if (!hasType || !hasData) {
                        System.out.println("❌ [2. Filter] 丢弃：缺少 type 或 data 字段。当前数据: " + obj.toJSONString());
                        return false;
                    }
                    System.out.println("✅ [2. Filter] 通过过滤，准备进入路由器。eventId: " + obj.getString("eventId"));
                    return true;
                });

        SingleOutputStreamOperator<JSONObject> routerStream = eventStream
                .keyBy(obj -> {
                    Long userId = obj.getLong("userId");
                    return userId == null ? -1L : userId;
                })
                .process(new KeyedProcessFunction<Long, JSONObject, JSONObject>() {

                    private ValueState<String> lastVisitDateState;
                    private ValueState<Boolean> uvFlagState;

                    @Override
                    public void open(Configuration parameters) {

                        lastVisitDateState = getRuntimeContext().getState(
                                new ValueStateDescriptor<>("lastVisitDate", String.class));

                        uvFlagState = getRuntimeContext().getState(
                                new ValueStateDescriptor<>("uvFlag", Boolean.class));
                    }

                    @Override
                    public void processElement(JSONObject value,
                                               Context ctx,
                                               Collector<JSONObject> out) throws Exception {

                        String type = value.getString("type");

                        Long userId = ctx.getCurrentKey();

                        // 时间
                        Long ts = value.getLong("timestamp");
                        if (ts == null) {
                            ts = System.currentTimeMillis();
                        }

                        LocalDate statDate = Instant.ofEpochMilli(ts)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();

                        String statDateStr = statDate.toString();

                        // =========================
                        // 跨天重置 UV 状态
                        // =========================

                        String lastDate = lastVisitDateState.value();

                        if (lastDate == null || !lastDate.equals(statDateStr)) {

                            lastVisitDateState.update(statDateStr);

                            uvFlagState.clear();
                        }

                        // =========================
                        // 构建 DailySummary
                        // =========================

                        JSONObject dailySummary = new JSONObject();

                        dailySummary.put("statDate", statDateStr);

                        dailySummary.put("pv", 0);
                        dailySummary.put("uv", 0);

                        dailySummary.put("newUsers", 0);
                        dailySummary.put("totalRevenue", BigDecimal.ZERO);
                        dailySummary.put("orderCount", 0);
                        dailySummary.put("tradeUsers", 0);
                        dailySummary.put("rechargeUsers", 0);
                        dailySummary.put("rechargeAmount", BigDecimal.ZERO);
                        dailySummary.put("refundAmount", BigDecimal.ZERO);
                        dailySummary.put("discountAmount", BigDecimal.ZERO);

                        // =========================
                        // Traffic 指标统计
                        // =========================

                        boolean isTrafficEvent =
                                EventType.SEARCH.getCode().equals(type)
                                        || EventType.BROWSE.getCode().equals(type);

                        if (isTrafficEvent) {

                            // PV
                            dailySummary.put("pv", 1);

                            // UV（日去重）
                            if (userId != -1L &&
                                    (uvFlagState.value() == null || !uvFlagState.value())) {

                                dailySummary.put("uv", 1);

                                uvFlagState.update(true);
                            }

                            // 输出 DAILY_SUMMARY
                            ctx.output(StreamRouteTags.DAILY_SUMMARY_TAG, dailySummary);
                        }

                        // 原有路由逻辑
                        if (EventType.SEARCH.getCode().equals(type)) {

                            ctx.output(StreamRouteTags.SEARCH_TAG, value);

                        } else if (EventType.BROWSE.getCode().equals(type)) {

                            ctx.output(StreamRouteTags.BROWSE_TAG, value);
                        }

                        out.collect(value);
                    }
                });


        routerStream.getSideOutput(StreamRouteTags.DAILY_SUMMARY_TAG)
                    .addSink(AdsSinkFactory.getDailySummarySink())
                    .name("SinkTo_t_daily_summary");
            routerStream.getSideOutput(StreamRouteTags.BROWSE_TAG).addSink(DwdSinkFactory.getBrowseLogSink()).name("SinkTo_t_browse_log");

            processHotWords(routerStream);
            processUserPreference(routerStream);

            env.execute("NXG Traffic Real-time Analytics (Unified Envelope)");
    }

    private static void processHotWords(DataStream<JSONObject> stream) {
        stream.filter(obj -> EventType.SEARCH.getCode().equals(obj.getString("type")))
                .map(obj -> obj.getJSONObject("data").getString("keyword"))
                .filter(StringUtils::isNotBlank)
                .windowAll(SlidingProcessingTimeWindows.of(Time.minutes(5), Time.seconds(10)))
                .apply(new AllWindowFunction<String, Map<String, Integer>, TimeWindow>() {
                    @Override
                    public void apply(TimeWindow window, Iterable<String> values, Collector<Map<String, Integer>> out) {
                        Map<String, Integer> counts = new HashMap<>();
                        values.forEach(word -> counts.merge(word, 1, Integer::sum));
                        out.collect(counts);
                    }
                }).name("HotWordStats");
    }

    /**
     * 业务：用户偏好分析与标签生成
     */
    private static void processUserPreference(DataStream<JSONObject> stream) {
        DataStream<Tuple5<Long, Long, Integer, String, String>> preferenceStream = stream.filter(obj -> {
                    String type = obj.getString("type");
                    // 💡 放行 LOGIN 事件，使其进入计算流
                    return EventType.SEARCH.getCode().equals(type)
                            || EventType.ADD_CART.getCode().equals(type)
                            || EventType.LIST.getCode().equals(type)
                            || EventType.LOGIN.getCode().equals(type);
                }).map(obj -> {
                    String type = obj.getString("type");

                    JSONObject data = obj.getJSONObject("data");

                    Long userId = obj.getLong("userId");
// 💡 核心防御：如果根目录没有，尝试去 data 对象里抓取
                    if (userId == null && data != null) {
                        userId = data.getLong("userId");
                    }
// 如果两处都没有（确实是未登录的匿名访客），再走 0L 兜底
                    if (userId == null) {
                        userId = 0L;
                    }

                    // 💡 分数计算规则：LOGIN 事件评分为 0，不破坏用户商品偏好池
                    int score = 0;
                    if (EventType.ADD_CART.getCode().equals(type)) {
                        score = 5;
                    } else if (EventType.SEARCH.getCode().equals(type) || EventType.LIST.getCode().equals(type)) {
                        score = 1;
                    }

                    Long categoryId = (data != null) ? data.getLong("categoryId") : null;
                    if (categoryId == null) categoryId = 0L;

                    // 💡 地域提取防御：若无省市，利用 IP 作为兜底展示
                    String province = (data != null) ? data.getString("province") : "";
                    String city = (data != null) ? data.getString("city") : "";
                    String fallbackIp = (data != null) ? data.getString("ip") : obj.getString("ip");

                    String area = (StringUtils.isNotBlank(province) && StringUtils.isNotBlank(city))
                            ? province + "-" + city
                            : (StringUtils.isNotBlank(province) ? province : fallbackIp);

                    return new Tuple5<>(userId, categoryId, score, area, area);
                })
                .returns(Types.TUPLE(Types.LONG, Types.LONG, Types.INT, Types.STRING, Types.STRING))
                .keyBy(t -> t.f0)
                // 💡 使用 Reduce 动态合并最新属性
                .reduce((t1, t2) -> new Tuple5<>(
                        t1.f0,                                         // userId
                        t2.f1 != 0L ? t2.f1 : t1.f1,                   // 优先保留有效的类目ID，防止被 LOGIN 的 0L 冲掉
                        t1.f2 + t2.f2,                                 // 偏好积分进行累加
                        StringUtils.isNotBlank(t2.f3) ? t2.f3 : t1.f3, // 覆盖更新为最新的 region
                        StringUtils.isNotBlank(t2.f4) ? t2.f4 : t1.f4  // 覆盖更新为最新的 last_login_area
                ));

        preferenceStream.addSink(AdsSinkFactory.getUserPersonaSink()).name("SinkTo_t_user_persona");
    }

    /**
     * 业务：实时 UV。
     * ✅ 已修复：增加了对映射出的 userId 的非空防御
     */
    private static void processRealTimeUV(DataStream<JSONObject> stream) {
        stream.filter(obj -> EventType.LOGIN.getCode().equals(obj.getString("type")))
                .map(obj -> {
                    Long userId = obj.getLong("userId");
                    return userId == null ? 0L : userId; // 👈 防御隐式拆箱带来的空指针
                })
                .returns(Types.LONG)
                .windowAll(TumblingProcessingTimeWindows.of(Time.days(1)))
                .process(new ProcessAllWindowFunction<Long, Long, TimeWindow>() {
                    @Override
                    public void process(Context context, Iterable<Long> elements, Collector<Long> out) {
                        Set<Long> uvSet = new HashSet<>();
                        elements.forEach(uvSet::add);
                        out.collect((long) uvSet.size());
                    }
                }).name("DailyUVCount");
    }
}