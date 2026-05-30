package com.nxg.mall.analytics.sink;

import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;

public class UserPortraitSinkFactory {

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
     * 获取用户画像明细 Sink (局部增量更新)
     */
    /**
     * 获取用户画像明细 Sink (局部增量更新)
     */
    public static SinkFunction<JSONObject> getUserInsightSink() {
        return JdbcSink.sink(
                "INSERT INTO t_user_portrait ( " +
                        "  user_id, username, total_browse_count, favorite_category, avg_stay_time, " +
                        "  total_spent, order_count, last_trade_time, last_login_time, last_login_ip, " +
                        "  active_level, province, city, update_time " +
                        ") VALUES ( " +
                        "  ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW() " +
                        ") " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  username = COALESCE(VALUES(username), username), " +

                        // ✨【核心修复 1】: 如果新传进来的类目不为空，则更新；如果新传进来的是 null，则保留原数据库的值
                        "  favorite_category = COALESCE(VALUES(favorite_category), favorite_category), " +

                        // ✨【核心修复 2】: 如果新传进来的停留时间不为空，则更新；为 null 则保留原值
                        "  avg_stay_time = COALESCE(VALUES(avg_stay_time), avg_stay_time), " +

                        "  total_browse_count = total_browse_count + COALESCE(VALUES(total_browse_count), 0), " +
                        "  total_spent = total_spent + COALESCE(VALUES(total_spent), 0), " +
                        "  order_count = order_count + COALESCE(VALUES(order_count), 0), " +
                        "  last_trade_time = COALESCE(VALUES(last_trade_time), last_trade_time), " +
                        "  last_login_time = COALESCE(VALUES(last_login_time), last_login_time), " +
                        "  last_login_ip = COALESCE(VALUES(last_login_ip), last_login_ip), " +
                        "  province = COALESCE(VALUES(province), province), " +
                        "  city = COALESCE(VALUES(city), city), " +
                        "  active_level = CASE " +
                        "                   WHEN (total_browse_count + COALESCE(VALUES(total_browse_count), 0)) > 100 THEN '高活跃' " +
                        "                   WHEN (total_browse_count + COALESCE(VALUES(total_browse_count), 0)) > 20 THEN '中活跃' " +
                        "                   ELSE '低活跃' " +
                        "                 END, " +
                        "  update_time = NOW()",
                (ps, obj) -> {
                    // 💡 下面的 Java 映射逻辑完全正确，包含占位符 1-13 对应，无需修改
                    Long userId = obj.getLong("userId");
                    if (userId == null) userId = 0L;

                    Integer totalBrowseCount = obj.getInteger("totalBrowseCount");
                    if (totalBrowseCount == null) totalBrowseCount = 0;

                    BigDecimal totalSpent = obj.getBigDecimal("totalSpent");
                    if (totalSpent == null) totalSpent = BigDecimal.ZERO;

                    Integer orderCount = obj.getInteger("orderCount");
                    if (orderCount == null) orderCount = 0;

                    Timestamp lastTradeTime = null;
                    String lastTradeTimeStr = obj.getString("lastTradeTime");
                    if (lastTradeTimeStr != null && !lastTradeTimeStr.isBlank()) {
                        lastTradeTime = Timestamp.valueOf(lastTradeTimeStr);
                    }

                    Timestamp lastLoginTime = null;
                    String lastLoginTimeStr = obj.getString("lastLoginTime");
                    if (lastLoginTimeStr != null && !lastLoginTimeStr.isBlank()) {
                        lastLoginTime = Timestamp.valueOf(lastLoginTimeStr);
                    }

                    String favCat = obj.getString("favoriteCategory");

                    System.out.println("🚀 [DEBUG-SINK] 准备执行 SQL 入库, userId=" + userId + ", favoriteCategory=" + favCat);

                    ps.setObject(1, userId, Types.BIGINT);
                    ps.setString(2, obj.getString("username"));
                    ps.setObject(3, totalBrowseCount, Types.INTEGER);
                    ps.setString(4, obj.getString("favoriteCategory"));
                    ps.setObject(5, obj.getInteger("avgStayTime"), Types.INTEGER);
                    ps.setBigDecimal(6, totalSpent);
                    ps.setObject(7, orderCount, Types.INTEGER);
                    ps.setObject(8, lastTradeTime, Types.TIMESTAMP);
                    ps.setObject(9, lastLoginTime, Types.TIMESTAMP);
                    ps.setString(10, obj.getString("lastLoginIp"));
                    ps.setString(11, obj.getString("activeLevel"));
                    ps.setString(12, obj.getString("province"));
                    ps.setString(13, obj.getString("city"));
                }, getExecOptions(), getConnOptions());
    }
}
