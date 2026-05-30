package com.nxg.mall.analytics.sink;

import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

public class SalesForecastSinkFactory {

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

    public static SinkFunction<JSONObject> getSalesForecastSink() {
        return JdbcSink.sink(
                "INSERT INTO ads_sales_forecast ( " +
                        "  forecast_month, model_name, expected_revenue, expected_growth_rate, seasonality_coefficient, create_time " +
                        ") VALUES ( " +
                        "  ?, ?, ?, ?, ?, NOW() " +
                        ") " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  model_name = COALESCE(VALUES(model_name), model_name), " +
                        "  expected_revenue = COALESCE(VALUES(expected_revenue), expected_revenue), " +
                        "  expected_growth_rate = COALESCE(VALUES(expected_growth_rate), expected_growth_rate), " +
                        "  seasonality_coefficient = COALESCE(VALUES(seasonality_coefficient), seasonality_coefficient), " +
                        "  create_time = NOW()",
                (ps, obj) -> {
                    ps.setString(1, obj.getString("forecastMonth"));
                    ps.setString(2, obj.getString("modelName"));
                    ps.setObject(3, obj.getBigDecimal("expectedRevenue"));
                    ps.setObject(4, obj.getBigDecimal("expectedGrowthRate"));
                    ps.setObject(5, obj.getBigDecimal("seasonalityCoefficient"));
                }, getExecOptions(), getConnOptions());
    }
}
