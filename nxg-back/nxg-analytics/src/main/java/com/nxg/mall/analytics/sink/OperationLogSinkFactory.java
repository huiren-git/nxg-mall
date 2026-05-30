package com.nxg.mall.analytics.sink;

import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.sql.Timestamp;
import java.sql.Types;

public class OperationLogSinkFactory {

    private static String jdbcUrl;
    private static String username;
    private static String password;

    public static void init(String url, String user, String pass) {
        jdbcUrl = url;
        username = user;
        password = pass;
    }

    private static JdbcExecutionOptions getExecOptions() {
        return JdbcExecutionOptions.builder()
                .withBatchSize(100)
                .withBatchIntervalMs(2000)
                .withMaxRetries(3)
                .build();
    }

    private static JdbcConnectionOptions getConnOptions() {
        if (jdbcUrl == null || username == null || password == null) {
            throw new IllegalArgumentException("OperationLogSinkFactory 未初始化！请先调用 OperationLogSinkFactory.init() 设置数据库连接信息。");
        }

        return new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                .withUrl(jdbcUrl)
                .withUsername(username)
                .withPassword(password)
                .withDriverName("com.mysql.cj.jdbc.Driver")
                .build();
    }

    public static SinkFunction<JSONObject> getOperationLogSink() {
        String sql = "INSERT INTO t_operation_log " +
                "(operator_account, operator_role, action_module, action_content, ip_address, create_time, target_type, target_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        return JdbcSink.sink(
                sql,
                (ps, obj) -> {
                    ps.setString(1, obj.getString("operatorName"));
                    ps.setString(2, obj.getString("role"));
                    ps.setString(3, obj.getString("actionType"));
                    ps.setString(4, obj.getString("detail"));
                    ps.setString(5, obj.getString("clientIp"));

                    Long opTime = obj.getLong("opTime");
                    if (opTime == null) {
                        ps.setNull(6, Types.TIMESTAMP);
                    } else {
                        ps.setTimestamp(6, new Timestamp(opTime));
                    }

                    ps.setString(7, obj.getString("targetType"));
                    ps.setString(8, obj.getString("targetId"));
                },
                getExecOptions(),
                getConnOptions()
        );
    }
}

