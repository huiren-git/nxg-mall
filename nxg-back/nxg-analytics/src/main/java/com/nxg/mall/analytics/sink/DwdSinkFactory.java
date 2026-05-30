package com.nxg.mall.analytics.sink;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.nxg.mall.common.enums.EventType;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class DwdSinkFactory {

    // 静态变量保存配置
    private static String jdbcUrl;
    private static String username;
    private static String password;

    // 时间解析器，用于处理前端传入的 "yyyy-MM-dd HH:mm:ss" 字符串
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 【新增】手动初始化配置方法。
     * 必须在 Flink 任务启动（env.execute）之前调用！
     */
    public static void init(String url, String user, String pass) {
        jdbcUrl = url;
        username = user;
        password = pass;
    }

    /**
     * 获取通用的批处理执行配置
     */
    private static JdbcExecutionOptions getExecOptions() {
        return JdbcExecutionOptions.builder()
                .withBatchSize(1)           // 每满 1000 条写入一次
                .withBatchIntervalMs(200)      // 或者每 200 毫秒写入一次
                .withMaxRetries(3)             // 写入失败重试 3 次
                .build();
    }

    /**
     * 动态获取连接配置
     */
    private static JdbcConnectionOptions getConnOptions() {
        // 增加前置校验，防止忘记初始化导致报错难以排查
        if (jdbcUrl == null || username == null || password == null) {
            throw new IllegalArgumentException("DwdSinkFactory 未初始化！请先调用 DwdSinkFactory.init() 设置数据库连接信息。");
        }

        return new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                .withUrl(jdbcUrl)
                .withUsername(username)
                .withPassword(password)
                .withDriverName("com.mysql.cj.jdbc.Driver")
                .build();
    }

    // 1. 浏览明细 Sink
    public static SinkFunction<JSONObject> getBrowseLogSink() {
        // 1. 修正表名为 t_user_browse_log
        // 2. 修正主键冲突策略：如果 event_id 冲突，说明用户刷新了页面或者补报了数据，我们应该更新离开时间和时长
        String sql = "INSERT INTO t_user_browse_log " +
                "(event_id, user_id, product_id, category_id, enter_time, leave_time, duration, client_ip, terminal, source_type) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE leave_time=VALUES(leave_time), duration=VALUES(duration)";

        return JdbcSink.sink(
                sql,
                (ps, obj) -> {
                    // 🌟 桩点 4：将整个绑定逻辑用 try-catch 包裹，强制暴露报错
                    try {
                        String eventId = obj.getString("eventId");
                        System.out.println("📥 [4. Sink Execution] 成功进入 JDBC Sink 绑定回调，当前处理 eventId: " + eventId);

                        JSONObject data = obj.getJSONObject("data");

                        // 转换进入时间
                        String enterTimeStr = data.getString("enterTime");
                        long enterTimeMs = LocalDateTime.parse(enterTimeStr, TIME_FORMATTER)
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli();

                        // 转换离开时间
                        String leaveTimeStr = data.getString("leaveTime");
                        Long leaveTimeMs = null;
                        if (StringUtils.isNotBlank(leaveTimeStr)) {
                            leaveTimeMs = LocalDateTime.parse(leaveTimeStr, TIME_FORMATTER)
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli();
                        }

                        // 1. 绑定位置 1: event_id
                        ps.setString(1, eventId);

                        // 2. 绑定位置 2: user_id
                        Long userId = obj.getLong("userId");
                        if (userId == null || userId == 0L) {
                            ps.setObject(2, null, Types.BIGINT);
                        } else {
                            ps.setObject(2, userId, Types.BIGINT);
                        }

                        // 3 & 4. 绑定位置: product_id, category_id
                        System.out.println("   -> 调试详情: productId=" + data.get("productId") + ", categoryId=" + data.get("categoryId"));
                        ps.setObject(3, data.getLong("productId"), Types.BIGINT);
                        ps.setObject(4, data.getLong("categoryId"), Types.BIGINT);

                        // 5 ~ 10. 其余参数绑定
                        ps.setTimestamp(5, new java.sql.Timestamp(enterTimeMs)); // ✅ JDBC 会自动把毫秒转换为 MySQL 认识的 "yyyy-MM-dd HH:mm:ss"

// 6. 绑定位置 6: leave_time (处理可能存在的 null 值)
                        if (leaveTimeMs != null) {
                            ps.setTimestamp(6, new java.sql.Timestamp(leaveTimeMs)); // ✅ 离开时间不为空时正常转换
                        } else {
                            ps.setNull(6, java.sql.Types.TIMESTAMP); // ✅ 离开时间为空时，安全写入库中的 NULL
                        }
                        ps.setObject(7, data.getInteger("duration"), Types.INTEGER);
                        ps.setString(8, obj.getString("ip"));
                        ps.setString(9, obj.getString("platform"));
                        ps.setString(10, data.getString("sourceType"));

                        System.out.println("✅ [4. Sink Execution] 参数全部绑定成功，PreparedStatement 准备提交 MySQL！eventId: " + eventId);

                    } catch (Exception e) {
                        System.err.println("💥💥💥 [4. Sink FATAL ERROR] 在执行 JDBC 参数绑定时发生致命崩溃 !!! 💥💥💥");
                        System.err.println("当前处理的完整原始 JSONObject 数据: " + obj.toJSONString());
                        e.printStackTrace(); // 强制打出红色堆栈信息
                        throw e; // 向上抛出让 Flink 感知
                    }
                },
                getExecOptions(),
                getConnOptions()
        );
    }

    // 2. 交易明细 Sink (处理下单与退款)
    public static SinkFunction<JSONObject> getTradeLogSink() {
        return JdbcSink.sink(
                "INSERT INTO dwd_trade_log (event_id, order_no, user_id, amount, discount_amount, type, event_time) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE event_id=VALUES(event_id)",
                (ps, obj) -> {
                    try {
                        String eventId = obj.getString("eventId");
                        if (StringUtils.isBlank(eventId)) {
                            eventId = "missing-" + UUID.randomUUID();
                        }

                        String type = obj.getString("type");

                        JSONObject data = obj.getJSONObject("data");
                        if (data == null) {
                            data = new JSONObject();
                        }

                        Long userId = obj.getLong("userId");
                        if (userId == null) {
                            userId = data.getLong("userId");
                        }
                        if (userId == null) {
                            userId = 0L;
                        }

                        BigDecimal amount = data.getBigDecimal("payAmount");
                        if (amount == null) {
                            amount = BigDecimal.ZERO;
                        }

                        BigDecimal discountAmount = data.getBigDecimal("discountAmount");
                        if (discountAmount == null) {
                            discountAmount = BigDecimal.ZERO;
                        }

                        if (EventType.ORDER_REFUND.getCode().equals(type)) {
                            amount = amount.negate();
                        }

                        String orderNo = data.getString("orderNo");
                        if (StringUtils.isBlank(orderNo)) {
                            orderNo = "UNKNOWN";
                        }

                        Long ts = obj.getLong("timestamp");
                        if (ts == null) {
                            ts = System.currentTimeMillis();
                        }

                        ps.setString(1, eventId);
                        ps.setString(2, orderNo);
                        ps.setObject(3, userId, Types.BIGINT);
                        ps.setBigDecimal(4, amount);
                        ps.setBigDecimal(5, discountAmount);
                        ps.setString(6, type);
                        ps.setTimestamp(7, new Timestamp(ts));
                    } catch (Exception e) {
                        System.err.println("DwdSinkFactory.getTradeLogSink 发生异常, eventId=" + obj.getString("eventId") + ", obj=" + obj.toJSONString());
                        throw e;
                    }
                }, getExecOptions(), getConnOptions());
    }

    // 3. 审计日志 Sink (注意：审计日志是扁平结构，没有 data 节点)
    public static SinkFunction<JSONObject> getAuditLogSink() {
        return JdbcSink.sink(
                "INSERT INTO dwd_audit_log  (operator_id, operator_name, action_type, target_type, target_id, client_ip, event_time) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                (ps, obj) -> {
                    try {
                        Long operatorId = obj.getLong("operatorId");
                        if (operatorId == null) {
                            operatorId = 0L;
                        }

                        Long opTime = obj.getLong("opTime");
                        if (opTime == null) {
                            opTime = System.currentTimeMillis();
                        }

                        ps.setObject(1, operatorId, Types.BIGINT);
                        ps.setString(2, obj.getString("operatorName"));
                        ps.setString(3, obj.getString("actionType"));
                        ps.setString(4, obj.getString("targetType"));
                        ps.setString(5, obj.getString("targetId"));
                        ps.setString(6, obj.getString("clientIp"));
                        ps.setTimestamp(7, new Timestamp(opTime));
                    } catch (Exception e) {
                        System.err.println("DwdSinkFactory.getAuditLogSink 发生异常, eventId=" + obj.getString("eventId") + ", obj=" + obj.toJSONString());
                        throw e;
                    }
                }, getExecOptions(), getConnOptions());
    }

    // 4. 搜索明细 Sink
    public static SinkFunction<JSONObject> getSearchLogSink() {
        return JdbcSink.sink(
                "INSERT INTO dwd_search_log (event_id, user_id, keyword, event_time) " +
                        "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE event_id=VALUES(event_id)",
                (ps, obj) -> {
                    try {
                        String eventId = obj.getString("eventId");
                        if (StringUtils.isBlank(eventId)) {
                            eventId = "missing-" + UUID.randomUUID();
                        }

                        JSONObject data = obj.getJSONObject("data");
                        if (data == null) {
                            data = new JSONObject();
                        }

                        Long userId = obj.getLong("userId");
                        if (userId == null) {
                            userId = data.getLong("userId");
                        }
                        if (userId == null) {
                            userId = 0L;
                        }

                        Long ts = obj.getLong("timestamp");
                        if (ts == null) {
                            ts = System.currentTimeMillis();
                        }

                        ps.setString(1, eventId);
                        ps.setObject(2, userId, Types.BIGINT);
                        ps.setString(3, data.getString("keyword"));
                        ps.setTimestamp(4, new Timestamp(ts));
                    } catch (Exception e) {
                        System.err.println("DwdSinkFactory.getSearchLogSink 发生异常, eventId=" + obj.getString("eventId") + ", obj=" + obj.toJSONString());
                        throw e;
                    }
                }, getExecOptions(), getConnOptions());
    }

    // 5. 充值明细 Sink
    public static SinkFunction<JSONObject> getRechargeLogSink() {
        return JdbcSink.sink(
                "INSERT INTO dwd_recharge_log (event_id, order_no, user_id, amount, event_time) " +
                        "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE event_id=VALUES(event_id)",
                (ps, obj) -> {
                    try {
                        String eventId = obj.getString("eventId");
                        if (StringUtils.isBlank(eventId)) {
                            eventId = "missing-" + UUID.randomUUID();
                        }

                        JSONObject data = obj.getJSONObject("data");
                        if (data == null) {
                            data = new JSONObject();
                        }

                        Long userId = obj.getLong("userId");
                        if (userId == null) {
                            userId = data.getLong("userId");
                        }
                        if (userId == null) {
                            userId = 0L;
                        }

                        String orderNo = data.getString("orderNo");
                        if (StringUtils.isBlank(orderNo)) {
                            orderNo = "UNKNOWN";
                        }

                        BigDecimal amount = data.getBigDecimal("amount");
                        if (amount == null) {
                            amount = BigDecimal.ZERO;
                        }

                        Long ts = obj.getLong("timestamp");
                        if (ts == null) {
                            ts = System.currentTimeMillis();
                        }

                        ps.setString(1, eventId);
                        ps.setString(2, orderNo);
                        ps.setObject(3, userId, Types.BIGINT);
                        ps.setBigDecimal(4, amount);
                        ps.setTimestamp(5, new Timestamp(ts));
                    } catch (Exception e) {
                        System.err.println("DwdSinkFactory.getRechargeLogSink 发生异常, eventId=" + obj.getString("eventId") + ", obj=" + obj.toJSONString());
                        throw e;
                    }
                }, getExecOptions(), getConnOptions());
    }

    // 6. 注册明细 Sink
    public static SinkFunction<JSONObject> getUserRegisterLogSink() {
        return JdbcSink.sink(
                "INSERT INTO dwd_user_register_log (event_id, user_id, username, email, register_ip, event_time) " +
                        "VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE event_id=VALUES(event_id)",
                (ps, obj) -> {
                    try {
                        String eventId = obj.getString("eventId");
                        if (StringUtils.isBlank(eventId)) {
                            eventId = "missing-" + UUID.randomUUID();
                        }

                        JSONObject data = obj.getJSONObject("data");
                        if (data == null) {
                            data = new JSONObject();
                        }

                        Long userId = obj.getLong("userId");
                        if (userId == null) {
                            userId = data.getLong("userId");
                        }
                        if (userId == null) {
                            userId = 0L;
                        }

                        Long ts = obj.getLong("timestamp");
                        if (ts == null) {
                            ts = System.currentTimeMillis();
                        }

                        ps.setString(1, eventId);
                        ps.setObject(2, userId, Types.BIGINT);
                        ps.setString(3, data.getString("username"));
                        ps.setString(4, data.getString("email"));
                        ps.setString(5, data.getString("registerIp"));
                        ps.setTimestamp(6, new Timestamp(ts));
                    } catch (Exception e) {
                        System.err.println("DwdSinkFactory.getUserRegisterLogSink 发生异常, eventId=" + obj.getString("eventId") + ", obj=" + obj.toJSONString());
                        throw e;
                    }
                }, getExecOptions(), getConnOptions());
    }

    // 7. 登录明细 Sink
    public static SinkFunction<JSONObject> getUserLoginLogSink() {
        return JdbcSink.sink(
                "INSERT INTO dwd_user_login_log (event_id, user_id, username, login_ip, terminal, status, fail_reason, event_time) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE event_id=VALUES(event_id)",
                (ps, obj) -> {
                    try {
                        String eventId = obj.getString("eventId");
                        if (StringUtils.isBlank(eventId)) {
                            eventId = "missing-" + UUID.randomUUID();
                        }

                        JSONObject data = obj.getJSONObject("data");
                        if (data == null) {
                            data = new JSONObject();
                        }

                        Long userId = obj.getLong("userId");
                        if (userId == null) {
                            userId = data.getLong("userId");
                        }
                        if (userId == null) {
                            userId = 0L;
                        }

                        Integer status = data.getInteger("status");
                        if (status == null) {
                            status = 1;
                        }

                        Long ts = obj.getLong("timestamp");
                        if (ts == null) {
                            ts = System.currentTimeMillis();
                        }

                        ps.setString(1, eventId);
                        ps.setObject(2, userId, Types.BIGINT);
                        ps.setString(3, data.getString("username"));
                        ps.setString(4, data.getString("loginIp"));
                        ps.setString(5, data.getString("terminal"));
                        ps.setObject(6, status, Types.INTEGER);
                        ps.setString(7, data.getString("failReason"));
                        ps.setTimestamp(8, new Timestamp(ts));
                    } catch (Exception e) {
                        System.err.println("DwdSinkFactory.getUserLoginLogSink 发生异常, eventId=" + obj.getString("eventId") + ", obj=" + obj.toJSONString());
                        throw e;
                    }
                }, getExecOptions(), getConnOptions());
    }
}
