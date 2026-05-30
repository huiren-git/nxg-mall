package com.nxg.mall.common.constant;

/**
 * Redis 常量类
 * 统一管理 Redis 键名及过期时间
 */
public final class RedisConstants {

    // ================= 验证码相关 =================
    /** 验证码缓存前缀 */
    public static final String CAPTCHA_PREFIX = "captcha:email:";

    /** ✨ 新增：用户注册验证码专属前缀 */
    public static final String CAPTCHA_REGISTER_PREFIX = "captcha:email:register:";

    /** 验证码过期时间（分） */
    public static final long CAPTCHA_EXPIRE_SECONDS = 5;

    /** 重置密码前缀 */
    public static final String RESET_TOKEN_PREFIX = "reset_token:";

    // ================= 注册失败 IP 限制相关 =================
    /** 注册失败 IP 计数前缀 */
    public static final String REG_FAIL_PREFIX = "reg_fail_ip:";
    /** 允许的最大失败次数 */
    public static final int MAX_FAIL_TIMES = 5;
    /** 失败计数有效期（天） */
    public static final long FAIL_COUNT_EXPIRE_DAYS = 1;

    /** IP 锁定时间（小时） */
    public static final long LOCK_EXPIRE_HOURS = 24;

    // ================= 购物车相关 =================
    /** 购物车 Hash 前缀 */
    public static final String CART_PREFIX = "cart:";

    // ================= 商品排行相关 =================
    /** 全站热卖榜 ZSET Key */
    public static final String RANK_GLOBAL_SALES = "rank:global:sales";
    
    /** 分类热卖榜 ZSET 前缀 */
    public static final String RANK_CATEGORY_SALES_PREFIX = "rank:category:";
    /** 分类热卖榜 ZSET 后缀 */
    public static final String RANK_CATEGORY_SALES_SUFFIX = ":sales";

    // ================= 防刷与接口限流相关 =================
    /** 商品接口每秒访问计数前缀 */
    public static final String PRODUCT_LIMIT_COUNT_PREFIX = "limit:count:product:";

    /** 商品接口 IP 封禁锁定前缀 */
    public static final String PRODUCT_LIMIT_LOCK_PREFIX = "limit:lock:product:";

    /** 每秒允许的最大访问次数 */
    public static final int MAX_PRODUCT_REQUEST_PER_SECOND = 5;

    /** 触发恶意刷接口后的锁定时长（小时） */
    public static final long PRODUCT_LOCK_EXPIRE_HOURS = 1;

    // ================= 异常安全大盘实时指标 =================
    /** 今日异常总数计数器 (String类型) */
    public static final String ANOMALY_TOTAL_TODAY = "anomaly:dashboard:total:today";

    /** 今日已拦截异常数计数器 (String类型) */
    public static final String ANOMALY_INTERCEPTED_COUNT = "anomaly:dashboard:intercepted:today";

    /** 当前系统实时 QPS (String类型，由限流层或网关动态更新) */
    public static final String SYSTEM_CURRENT_QPS = "system:metrics:current:qps";

    /** 系统平均响应时间 RT (String类型) */
    public static final String SYSTEM_AVG_RT = "system:metrics:avg:rt";

    /** 系统健康评分 (String类型) */
    public static final String SYSTEM_HEALTH_SCORE = "system:metrics:health:score";

    /** Kafka/Flink 聚合后的实时窗口异常数 (Hash类型，Field为 "20:00", Value为数量) */
    public static final String ANOMALY_CHART_ACTUAL_HASH = "metrics:anomaly:window:actual";

    /** 系统历史基线数据 (Hash类型，Field为 "20:00", Value为基线数量) */
    public static final String ANOMALY_CHART_BASELINE_HASH = "metrics:anomaly:window:baseline";

    private RedisConstants() {
        // 私有构造函数，防止实例化
    }
}