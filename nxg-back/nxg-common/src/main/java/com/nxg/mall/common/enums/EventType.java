package com.nxg.mall.common.enums;

import lombok.Getter;

@Getter
public enum EventType {
    // === 1. 流量与行为类 (nxg_traffic_log) ===
    PV("PV", "页面访问"),
    BROWSE("BROWSE", "商品浏览"),
    CLICK("CLICK", "点击事件"),
    SEARCH("SEARCH", "搜索行为"),
    LIST("LIST", "列表查看"),
    ADD_CART("ADD_CART", "加入购物车"),

    // === 2. 业务与交易类 (nxg_business_event) ===
    REGISTER("REGISTER", "用户注册"),
    LOGIN("LOGIN", "用户登录"),
    USER_RECHARGE("USER_RECHARGE", "账户充值"),
    ORDER_CREATE("ORDER_CREATE", "订单创建"),
    ORDER_REFUND("ORDER_REFUND", "订单退款"),
    PAYMENT("PAYMENT", "用户付款"),
    ORDER_STATUS_UPDATE("ORDER_STATUS_UPDATE", "订单状态变更(待评价/已完成)"),
    FORECAST_UPDATE("FORECAST_UPDATE", "销售预测数据调整"),
    UPDATE_SALES("UPDATE_SALES", "商品销量等数据变更"),

    // === 3. 系统与审计类 (nxg_system_audit) ===
    OPERATE("OPERATE", "操作审计");

    private final String code;
    private final String description;

    EventType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据字符串代码获取枚举对象（用于解析 Kafka 数据）
     */
    public static EventType getByCode(String code) {
        for (EventType type : EventType.values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
