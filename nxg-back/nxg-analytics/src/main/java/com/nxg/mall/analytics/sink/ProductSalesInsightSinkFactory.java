package com.nxg.mall.analytics.sink;

import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.sql.Types;

public class ProductSalesInsightSinkFactory {

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

    public static SinkFunction<JSONObject> getProductSalesInsightSink() {
        return JdbcSink.sink(
                "INSERT INTO t_product_sales_insight ( " +
                        "  product_id, product_name, current_stock, avg_daily_sales, evaluation_score, " +
                        "  health_status, risk_warning, suggested_action, days_to_out_of_stock, restock_deadline_days, update_time " +
                        ") VALUES ( " +
                        "  ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW() " +
                        ") " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  product_name = COALESCE(VALUES(product_name), product_name), " +
                        "  current_stock = current_stock + COALESCE(VALUES(current_stock), 0), " +
                        "  avg_daily_sales = COALESCE(VALUES(avg_daily_sales), avg_daily_sales), " +
                        "  evaluation_score = COALESCE(VALUES(evaluation_score), evaluation_score), " +
                        "  health_status = COALESCE(VALUES(health_status), health_status), " +
                        "  risk_warning = COALESCE(VALUES(risk_warning), risk_warning), " +
                        "  suggested_action = COALESCE(VALUES(suggested_action), suggested_action), " +
                        "  days_to_out_of_stock = COALESCE(VALUES(days_to_out_of_stock), days_to_out_of_stock), " +
                        "  restock_deadline_days = COALESCE(VALUES(restock_deadline_days), restock_deadline_days), " +
                        "  update_time = NOW()",
                (ps, obj) -> {
                    Long productId = obj.getLong("productId");
                    if (productId == null) productId = 0L;

                    Integer currentStock = obj.getInteger("currentStock");
                    if (currentStock == null) currentStock = 0;

                    ps.setObject(1, productId, Types.BIGINT);
                    ps.setString(2, obj.getString("productName"));
                    ps.setObject(3, currentStock, Types.INTEGER);
                    ps.setObject(4, obj.getInteger("avgDailySales"), Types.INTEGER);
                    ps.setObject(5, obj.getInteger("evaluationScore"), Types.INTEGER);
                    ps.setString(6, obj.getString("healthStatus"));
                    ps.setString(7, obj.getString("riskWarning"));
                    ps.setString(8, obj.getString("suggestedAction"));
                    ps.setObject(9, obj.getInteger("daysToOutOfStock"), Types.INTEGER);
                    ps.setObject(10, obj.getInteger("restockDeadlineDays"), Types.INTEGER);
                }, getExecOptions(), getConnOptions());
    }

    public static SinkFunction<JSONObject> getProductMetricDetailSink() {
        return JdbcSink.sink(
                "INSERT INTO t_product_metric_detail ( " +
                        "  product_id, metric_dim, current_value, mom_change, diagnostic_result " +
                        ") VALUES ( " +
                        "  ?, ?, ?, ?, ? " +
                        ") " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  current_value = COALESCE(VALUES(current_value), current_value), " +
                        "  mom_change = COALESCE(VALUES(mom_change), mom_change), " +
                        "  diagnostic_result = COALESCE(VALUES(diagnostic_result), diagnostic_result)",
                (ps, obj) -> {
                    Long productId = obj.getLong("productId");
                    if (productId == null) productId = 0L;

                    ps.setObject(1, productId, Types.BIGINT);
                    ps.setString(2, obj.getString("metricDim"));
                    ps.setString(3, obj.getString("currentValue"));
                    ps.setString(4, obj.getString("momChange"));
                    ps.setString(5, obj.getString("diagnosticResult"));
                }, getExecOptions(), getConnOptions());
    }

    public static SinkFunction<JSONObject> getProductSalesTrendSink() {
        return JdbcSink.sink(
                "INSERT INTO t_product_sales_trend ( " +
                        "  product_id, period_name, sales_volume, is_forecast, sort_order " +
                        ") VALUES ( " +
                        "  ?, ?, ?, ?, ? " +
                        ") " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  sales_volume = sales_volume + COALESCE(VALUES(sales_volume), 0), " + // 发生重复时累加销量
                        "  sort_order = COALESCE(VALUES(sort_order), sort_order)",
                (ps, obj) -> {
                    Long productId = obj.getLong("productId");
                    if (productId == null) productId = 0L;

                    ps.setObject(1, productId, Types.BIGINT);
                    ps.setString(2, obj.getString("periodName"));

                    Integer salesVolume = obj.getInteger("salesVolume");
                    ps.setObject(3, salesVolume == null ? 0 : salesVolume, Types.INTEGER);

                    Integer isForecast = obj.getInteger("isForecast");
                    ps.setObject(4, isForecast == null ? 0 : isForecast, Types.TINYINT);

                    Integer sortOrder = obj.getInteger("sortOrder");
                    ps.setObject(5, sortOrder == null ? 0 : sortOrder, Types.INTEGER);
                }, getExecOptions(), getConnOptions());
    }
}
