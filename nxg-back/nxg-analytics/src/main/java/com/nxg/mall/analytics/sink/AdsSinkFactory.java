package com.nxg.mall.analytics.sink;

import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple5;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Types;

public class AdsSinkFactory {

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

    /**
     * 获取商品分析大盘 Sink
     */
    public static SinkFunction<JSONObject> getProductAnalysisSink() {
        return JdbcSink.sink(
                "INSERT INTO ads_product_analysis ( " +
                        "dt, product_name, category, stock, status, sales_volume, turnover_rate, price, update_time " +
                        ") VALUES (?, ?, ?, ?, '正常', ?, 0, ?, NOW()) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "category = VALUES(category), " +
                        "stock = VALUES(stock), " +
                        "sales_volume = sales_volume + VALUES(sales_volume), " +
                        "turnover_rate = IF(stock > 0, sales_volume / stock, 0), " +
                        "status = CASE " +
                        "           WHEN stock <= 50 THEN '库存预警' " +
                        "           WHEN sales_volume > 100 THEN '热销中' " +
                        "           ELSE '正常' " +
                        "         END, " +
                        "price = VALUES(price), " +
                        "update_time = NOW()",
                (ps, obj) -> {
                    String dtStr = obj.getString("dt");
                    Date dt = dtStr != null && !dtStr.isBlank() ? Date.valueOf(dtStr) : null;

                    Integer stock = obj.getInteger("stock");
                    if (stock == null) stock = 0;

                    Integer salesVolume = obj.getInteger("salesVolume");
                    if (salesVolume == null) salesVolume = 0;

                    BigDecimal price = obj.getBigDecimal("price");
                    if (price == null) price = BigDecimal.ZERO;

                    ps.setObject(1, dt, Types.DATE);
                    ps.setString(2, obj.getString("productName"));
                    ps.setString(3, obj.getString("category"));
                    ps.setObject(4, stock, Types.INTEGER);
                    ps.setObject(5, salesVolume, Types.INTEGER);
                    ps.setBigDecimal(6, price);
                }, getExecOptions(), getConnOptions());
    }

    /**
     * 获取每日交易大盘汇总 Sink
     */
    public static SinkFunction<JSONObject> getSalesDailyStatsSink() {
        return JdbcSink.sink(
                "INSERT INTO ads_sales_daily_stats ( " +
                        "  stat_date, total_sales_amount, sales_trend, total_order_count, order_trend, " +
                        "  pending_order_count, pending_trend, avg_order_value, avg_value_trend, update_time " +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW()) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  total_sales_amount = total_sales_amount + COALESCE(VALUES(total_sales_amount), 0), " +
                        "  total_order_count = total_order_count + COALESCE(VALUES(total_order_count), 0), " +
                        "  pending_order_count = pending_order_count + COALESCE(VALUES(pending_order_count), 0), " +
                        "  sales_trend = COALESCE(VALUES(sales_trend), sales_trend), " +
                        "  order_trend = COALESCE(VALUES(order_trend), order_trend), " +
                        "  pending_trend = COALESCE(VALUES(pending_trend), pending_trend), " +
                        "  avg_value_trend = COALESCE(VALUES(avg_value_trend), avg_value_trend), " +
                        // 💡 核心修复：利用前两行已经更新的最新总额和最新单量，直接计算出最新客单价
                        // 使用 IF 防止总单量为 0 导致除零异常
                        "  avg_order_value = total_sales_amount / IF(total_order_count = 0, 1, total_order_count), " +
                        "  update_time = NOW()",
                (ps, obj) -> {
                    String statDateStr = obj.getString("statDate");
                    Date statDate = statDateStr != null && !statDateStr.isBlank() ? Date.valueOf(statDateStr) : null;

                    BigDecimal totalSalesAmount = obj.getBigDecimal("totalSalesAmount");
                    if (totalSalesAmount == null) totalSalesAmount = BigDecimal.ZERO;

                    Integer totalOrderCount = obj.getInteger("totalOrderCount");
                    if (totalOrderCount == null) totalOrderCount = 0;

                    Integer pendingOrderCount = obj.getInteger("pendingOrderCount");
                    if (pendingOrderCount == null) pendingOrderCount = 0;

                    // 💡 在 INSERT（首次插入）时，客单价如果为空，这里可以给 0
                    // UPDATE 时这个 0 将被抛弃，转由 SQL 动态计算
                    BigDecimal avgOrderValue = obj.getBigDecimal("avgOrderValue");
                    if (avgOrderValue == null) avgOrderValue = BigDecimal.ZERO;

                    ps.setObject(1, statDate, Types.DATE);
                    ps.setBigDecimal(2, totalSalesAmount);
                    ps.setString(3, obj.getString("salesTrend"));
                    ps.setObject(4, totalOrderCount, Types.INTEGER);
                    ps.setString(5, obj.getString("orderTrend"));
                    ps.setObject(6, pendingOrderCount, Types.INTEGER);
                    ps.setString(7, obj.getString("pendingTrend"));
                    ps.setBigDecimal(8, avgOrderValue);
                    ps.setString(9, obj.getString("avgValueTrend"));
                }, getExecOptions(), getConnOptions());
    }

    /**
     * 获取销售小时趋势 Sink
     */
    public static SinkFunction<JSONObject> getSalesHourlyTrendSink() {
        return JdbcSink.sink(
                "INSERT INTO ads_sales_hourly_trend ( " +
                        "  stat_date, hour_node, sales_amount, order_count " +
                        ") VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  sales_amount = sales_amount + COALESCE(VALUES(sales_amount), 0), " +
                        "  order_count = order_count + COALESCE(VALUES(order_count), 0)",
                (ps, obj) -> {
                    String statDateStr = obj.getString("statDate");
                    Date statDate = statDateStr != null && !statDateStr.isBlank() ? Date.valueOf(statDateStr) : null;

                    BigDecimal salesAmount = obj.getBigDecimal("salesAmount");
                    if (salesAmount == null) salesAmount = BigDecimal.ZERO;

                    Integer orderCount = obj.getInteger("orderCount");
                    if (orderCount == null) orderCount = 0;

                    ps.setObject(1, statDate, Types.DATE);
                    ps.setString(2, obj.getString("hourNode"));
                    ps.setBigDecimal(3, salesAmount);
                    ps.setObject(4, orderCount, Types.INTEGER);
                }, getExecOptions(), getConnOptions());
    }

    private static JdbcExecutionOptions getExecOptions() {
        return JdbcExecutionOptions.builder().withBatchSize(500).withBatchIntervalMs(200).withMaxRetries(3).build();
    }

    /**
     * 获取用户统计宽表 Sink
     */
    public static SinkFunction<JSONObject> getCustomerStatSink() {
        return JdbcSink.sink(
                "INSERT INTO t_customer_stat_wide (user_id, nickname, email, avatar, balance, order_count, total_spend) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                        "nickname = IFNULL(VALUES(nickname), nickname), " +
                        "email = IFNULL(VALUES(email), email), " +
                        "avatar = IFNULL(VALUES(avatar), avatar), " +
                        "balance = balance + VALUES(balance), " + // 💡 1. 核心修改：在原有余额基础上，累加本次变动额（正数加，负数减）
                        "order_count = order_count + VALUES(order_count), " +
                        "total_spend = total_spend + VALUES(total_spend)",
                (ps, obj) -> {
                    Long userId = obj.getLong("userId");
                    if (userId == null) userId = 0L;

                    Long orderCount = obj.getLong("orderCount");
                    if (orderCount == null) orderCount = 0L;

                    BigDecimal totalSpend = obj.getBigDecimal("totalSpend");
                    if (totalSpend == null) totalSpend = BigDecimal.ZERO;

                    // 💡 2. 核心修改：从 JSON 中提取各类金额，并在 Java 端计算出本次余额的【净变动值】
                    BigDecimal rechargeAmount = obj.getBigDecimal("rechargeAmount");
                    BigDecimal payAmount = obj.getBigDecimal("payAmount");
                    BigDecimal refundAmount = obj.getBigDecimal("refundAmount");

                    BigDecimal balanceChange = BigDecimal.ZERO;
                    if (rechargeAmount != null) {
                        balanceChange = balanceChange.add(rechargeAmount);     // 充值：余额增加
                    }
                    if (payAmount != null) {
                        balanceChange = balanceChange.subtract(payAmount);     // 支付：余额减少
                    }
                    if (refundAmount != null) {
                        balanceChange = balanceChange.add(refundAmount);       // 退款：余额增加
                    }

                    ps.setObject(1, userId, Types.BIGINT);
                    ps.setString(2, obj.getString("nickname"));
                    ps.setString(3, obj.getString("email"));
                    ps.setString(4, obj.getString("avatar"));
                    ps.setBigDecimal(5, balanceChange); // 💡 3. 将计算好的净变动值绑定到第 5 个占位符
                    ps.setObject(6, orderCount, Types.BIGINT);
                    ps.setBigDecimal(7, totalSpend);
                }, getExecOptions(), getConnOptions());
    }
    /**
     * 获取大盘日指标汇总 Sink（已补全 UV、成交用户、充值用户字段）
     */
    public static SinkFunction<JSONObject> getDailySummarySink() {
        return JdbcSink.sink(
                "INSERT INTO t_daily_summary (stat_date, pv, uv, new_users, total_revenue, order_count, trade_users, recharge_users, recharge_amount, refund_amount, discount_amount) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                        "pv = pv + VALUES(pv), " +
                        "uv = uv + VALUES(uv), " +
                        "new_users = new_users + VALUES(new_users), " +
                        "total_revenue = IFNULL(total_revenue, 0) + VALUES(total_revenue), " +
                        "order_count = order_count + VALUES(order_count), " +
                        "trade_users = trade_users + VALUES(trade_users), " +
                        "recharge_users = recharge_users + VALUES(recharge_users), " +
                        "recharge_amount = IFNULL(recharge_amount, 0) + VALUES(recharge_amount), " +
                        "refund_amount = IFNULL(refund_amount, 0) + VALUES(refund_amount), " +
                        "discount_amount = IFNULL(discount_amount, 0) + VALUES(discount_amount)",
                (ps, obj) -> {
                    String statDateStr = obj.getString("statDate");
                    Date statDate = statDateStr != null && !statDateStr.isBlank() ? Date.valueOf(statDateStr) : null;

                    ps.setObject(1, statDate, Types.DATE);
                    ps.setObject(2, obj.getInteger("pv"), Types.INTEGER);
                    ps.setObject(3, obj.getInteger("uv"), Types.INTEGER);
                    ps.setObject(4, obj.getInteger("newUsers"), Types.INTEGER);
                    ps.setBigDecimal(5, obj.getBigDecimal("totalRevenue"));
                    ps.setObject(6, obj.getInteger("orderCount"), Types.INTEGER);
                    ps.setObject(7, obj.getInteger("tradeUsers"), Types.INTEGER);
                    ps.setObject(8, obj.getInteger("rechargeUsers"), Types.INTEGER);
                    ps.setBigDecimal(9, obj.getBigDecimal("rechargeAmount"));
                    ps.setBigDecimal(10, obj.getBigDecimal("refundAmount"));
                    ps.setBigDecimal(11, obj.getBigDecimal("discountAmount"));
                }, getExecOptions(), getConnOptions());
    }

    public static SinkFunction<Tuple5<Long, Long, Integer, String, String>> getUserPersonaSink() {
        return JdbcSink.sink(
                "INSERT INTO t_user_persona (user_id, region, purchasing_power, tags, last_login_area, update_time) " +
                        "VALUES (?, ?, ?, ?, ?, NOW()) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  region = COALESCE(VALUES(region), region), " +
                        "  tags = COALESCE(VALUES(tags), tags), " +
                        "  purchasing_power = COALESCE(VALUES(purchasing_power), purchasing_power), " +
                        "  last_login_area = COALESCE(VALUES(last_login_area), last_login_area), " +
                        "  update_time = NOW()",
                (ps, tuple) -> {
                    Long userId = tuple.f0;
                    Long categoryId = tuple.f1;
                    Integer score = tuple.f2;
                    String region = tuple.f3;          // 💡 拿到动态提取的地域
                    String lastLoginArea = tuple.f4;   // 💡 拿到动态提取的最后登录区域

                    // 核心画像标签逻辑：根据实时累加的偏好得分，动态映射为标签
                    String tag = "普通用户";
                    String purchasingPower = "中";
                    if (score > 50) {
                        tag = "核心发烧友_品类" + categoryId;
                        purchasingPower = "高";
                    } else if (score > 10) {
                        tag = "潜在意向高_品类" + categoryId;
                    }

                    ps.setLong(1, userId);
                    ps.setString(2, region);          // 填入真实 region
                    ps.setString(3, purchasingPower);
                    ps.setString(4, tag);
                    ps.setString(5, lastLoginArea);   // 填入真实 last_login_area
                },
                getExecOptions(),
                getConnOptions()
        );
    }
}
