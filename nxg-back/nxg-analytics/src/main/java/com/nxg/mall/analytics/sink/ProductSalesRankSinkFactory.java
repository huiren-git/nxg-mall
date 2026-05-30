package com.nxg.mall.analytics.sink;

import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.math.BigDecimal;
import java.sql.Types;

public class ProductSalesRankSinkFactory {

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
     * 获取商品销量排行 Sink
     */
    public static SinkFunction<JSONObject> getProductSalesRankSink() {
        return JdbcSink.sink(
                "INSERT INTO t_product_sales_rank ( " +
                        "  product_id, product_name, main_image, category, sales_volume, sales_amount, " +
                        "  product_pv, order_count, update_time " +
                        ") VALUES ( " +
                        "  ?, ?, ?, ?, ?, ?, ?, ?, NOW() " +
                        ") " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  product_name = COALESCE(VALUES(product_name), product_name), " +
                        "  main_image = COALESCE(VALUES(main_image), main_image), " +
                        "  category = COALESCE(VALUES(category), category), " +
                        "  sales_volume = sales_volume + COALESCE(VALUES(sales_volume), 0), " +
                        "  sales_amount = sales_amount + COALESCE(VALUES(sales_amount), 0), " +
                        "  product_pv = product_pv + COALESCE(VALUES(product_pv), 0), " +
                        "  order_count = order_count + COALESCE(VALUES(order_count), 0), " +
                        "  update_time = NOW()",

                (ps, obj) -> {

                    Long productId = obj.getLong("productId");
                    if (productId == null) productId = 0L;

                    Integer salesVolume = obj.getInteger("salesVolume");
                    if (salesVolume == null) salesVolume = 0;

                    BigDecimal salesAmount = obj.getBigDecimal("salesAmount");
                    if (salesAmount == null) salesAmount = BigDecimal.ZERO;

                    Integer productPv = obj.getInteger("productPv");
                    if (productPv == null) productPv = 0;

                    Integer orderCount = obj.getInteger("orderCount");
                    if (orderCount == null) orderCount = 0;

                    ps.setObject(1, productId, Types.BIGINT);
                    ps.setString(2, obj.getString("productName"));
                    ps.setString(3, obj.getString("mainImage"));
                    ps.setString(4, obj.getString("category"));
                    ps.setObject(5, salesVolume, Types.INTEGER);
                    ps.setBigDecimal(6, salesAmount);
                    ps.setObject(7, productPv, Types.INTEGER);
                    ps.setObject(8, orderCount, Types.INTEGER);

                }, getExecOptions(), getConnOptions());
    }
}
