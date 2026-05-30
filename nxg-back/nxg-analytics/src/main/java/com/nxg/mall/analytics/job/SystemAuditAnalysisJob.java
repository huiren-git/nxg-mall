package com.nxg.mall.analytics.job;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.nxg.mall.analytics.constant.StreamRouteTags;
import com.nxg.mall.analytics.sink.DwdSinkFactory;
import com.nxg.mall.analytics.sink.OperationLogSinkFactory;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemAuditAnalysisJob {
    private static final Logger LOG = LoggerFactory.getLogger(SystemAuditAnalysisJob.class);

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 审计日志涉及合规性，建议 Checkpoint 开启强一致性模式
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
                .setTopics("nxg_system_audit")
                .setGroupId("nxg_audit_analysis_group")
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<String> rawStream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Audit Source");

        // 解析扁平化的审计对象
        DataStream<JSONObject> auditStream = rawStream
                .map(json -> {
                    try {
                        return JSON.parseObject(json);
                    } catch (Exception e) {
                        LOG.error("审计日志格式错误: {}", json);
                        return null;
                    }
                })
                .filter(obj -> obj != null);

        SingleOutputStreamOperator<JSONObject> routerStream = auditStream.process(new ProcessFunction<JSONObject, JSONObject>() {
            @Override
            public void processElement(JSONObject value, Context ctx, Collector<JSONObject> out) {
                // 审计日志通常全部入库，不需要像业务流那样根据类型 switch
                // 直接将所有有效的审计对象发送到审计侧输出流
                ctx.output(StreamRouteTags.AUDIT_TAG, value);

                // 同时发送到主流，供下游的实时告警和风控逻辑使用
                out.collect(value);
            }
        });

        // 调用 SinkFactory 写入数据库 (t_system_audit_log)
        routerStream.getSideOutput(StreamRouteTags.AUDIT_TAG)
                .addSink(DwdSinkFactory.getAuditLogSink())
                .name("SinkTo_t_system_audit_log");

        // 调用新的 OperationLogSinkFactory 写入操作日志表 (t_operation_log)
        routerStream.getSideOutput(StreamRouteTags.AUDIT_TAG)
                .addSink(OperationLogSinkFactory.getOperationLogSink())
                .name("SinkTo_t_operation_log");

        // =========================================================================
        // === 实时分析逻辑 (使用 routerStream) ===
        // =========================================================================

        // 1. 高危操作实时监控
        processHighRiskOperations(routerStream);

        // 2. 统计异常登录或高频操作
        processSecurityAlerts(routerStream);

        env.execute("NXG System Audit Analysis Job");
    }

    /**
     * 业务：高危操作实时输出 (可对接钉钉/邮件告警)
     */
    private static void processHighRiskOperations(DataStream<JSONObject> stream) {
        stream.filter(obj -> {
                    String actionType = obj.getString("actionType");
                    // 忽略大小写，匹配所有高危操作类型
                    return actionType != null &&
                            (actionType.toUpperCase().contains("DELETE") ||
                                    actionType.toUpperCase().contains("UPDATE"));
                }).map(obj -> String.format("【高危操作告警】管理员: %s, 操作: %s, 目标ID: %s",
                        obj.getString("operatorName"),
                        obj.getString("detail") != null ? obj.getString("detail") : obj.getString("actionType"),
                        obj.getString("targetId")))
                .print("High-Risk-Alert");
    }

    /**
     * 业务：安全风控 - 统计 1 分钟内操作频率过高的账户
     */
    private static void processSecurityAlerts(DataStream<JSONObject> stream) {
        stream.keyBy(obj -> obj.getLong("operatorId"))
                .window(TumblingProcessingTimeWindows.of(Time.minutes(1)))
                .apply(new org.apache.flink.streaming.api.functions.windowing.WindowFunction<JSONObject, String, Long, TimeWindow>() {
                    @Override
                    public void apply(Long userId, TimeWindow window, Iterable<JSONObject> input, Collector<String> out) {
                        long count = 0;
                        for (JSONObject ignored : input) count++;
                        if (count > 100) { // 阈值设定，1分钟超过100次操作判定为异常
                            out.collect("【安全警告】账号ID: " + userId + " 在一分钟内操作超过100次，疑似脚本攻击！");
                        }
                    }
                }).print("Security-Watch");
    }
}
