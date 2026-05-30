package com.nxg.mall.analytics.sink;

import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.sql.Types;

public class AnomalyLogSinkFactory {

    // 🌟 动态获取：优先读取容器环境变量，读取不到则使用本地 localhost 默认值
    private static final String JDBC_URL = System.getenv("SPRING_DATASOURCE_URL") != null
            ? System.getenv("SPRING_DATASOURCE_URL")
            : "jdbc:mysql://localhost:3306/nxg_analytics?useSSL=false&serverTimezone=UTC&rewriteBatchedStatements=true";

    private static final String USERNAME = System.getenv("SPRING_DATASOURCE_USERNAME") != null
            ? System.getenv("SPRING_DATASOURCE_USERNAME")
            : "root";

    private static final String PASSWORD = System.getenv("SPRING_DATASOURCE_PASSWORD") != null
            ? System.getenv("SPRING_DATASOURCE_PASSWORD")
            : "root";

    private static JdbcConnectionOptions getConnOptions() {
        return new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                .withUrl(JDBC_URL).withDriverName("com.mysql.cj.jdbc.Driver")
                .withUsername(USERNAME).withPassword(PASSWORD).build();
    }

    private static JdbcExecutionOptions getExecOptions() {
        return JdbcExecutionOptions.builder()
                .withBatchSize(100)
                .withBatchIntervalMs(2000)
                .withMaxRetries(3)
                .build();
    }

    /**
     * 获取异常监控记录 Sink (幂等写入)
     */
    public static SinkFunction<JSONObject> getAnomalyLogSink() {
        return JdbcSink.sink(
                "INSERT INTO t_anomaly_log ( " +
                        "  log_no, anomaly_type, target_obj, risk_level, detail_desc, status " +
                        ") VALUES ( " +
                        "  ?, ?, ?, ?, ?, ? " +
                        ") " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  detail_desc = VALUES(detail_desc), " +
                        "  status = VALUES(status)",
                (ps, obj) -> {
                    Integer status = obj.getInteger("status");
                    if (status == null) status = 0;

                    ps.setString(1, obj.getString("logNo"));
                    ps.setString(2, obj.getString("anomalyType"));
                    ps.setString(3, obj.getString("targetObj"));
                    ps.setString(4, obj.getString("riskLevel"));
                    ps.setString(5, obj.getString("detailDesc"));
                    ps.setObject(6, status, Types.INTEGER);
                }, getExecOptions(), getConnOptions());
    }
}
