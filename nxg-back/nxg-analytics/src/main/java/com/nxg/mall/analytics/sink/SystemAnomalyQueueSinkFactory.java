package com.nxg.mall.analytics.sink;

import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.math.BigDecimal;
import java.sql.Types;

public class SystemAnomalyQueueSinkFactory {

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
     * 获取系统异常事件队列 Sink (追加写入)
     */
    public static SinkFunction<JSONObject> getSystemAnomalyQueueSink() {
        return JdbcSink.sink(
                "INSERT INTO t_system_anomaly_queue ( " +
                        "  business_type, anomaly_type, target_object, buyer_name, " +
                        "  order_amount, risk_level, description, status " +
                        ") VALUES ( " +
                        "  ?, ?, ?, ?, ?, ?, ?, ? " +
                        ")",
                (ps, obj) -> {
                    Integer businessType = obj.getInteger("businessType");
                    if (businessType == null) businessType = 0;

                    BigDecimal orderAmount = obj.getBigDecimal("orderAmount");
                    if (orderAmount == null) orderAmount = BigDecimal.ZERO;

                    Integer status = obj.getInteger("status");
                    if (status == null) status = 0;

                    ps.setObject(1, businessType, Types.INTEGER);
                    ps.setString(2, obj.getString("anomalyType"));
                    ps.setString(3, obj.getString("targetObject"));
                    ps.setString(4, obj.getString("buyerName"));
                    ps.setBigDecimal(5, orderAmount);
                    ps.setString(6, obj.getString("riskLevel"));
                    ps.setString(7, obj.getString("description"));
                    ps.setObject(8, status, Types.INTEGER);
                }, getExecOptions(), getConnOptions());
    }
}
