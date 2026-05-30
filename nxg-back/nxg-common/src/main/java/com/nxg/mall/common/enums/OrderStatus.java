package com.nxg.mall.common.enums;

import lombok.Getter;

/**
 * 订单状态枚举
 */
@Getter
public enum OrderStatus {

    WAIT_PAY(0, "待支付"),
    WAIT_DELIVER(1, "待发货"),
    WAIT_RECEIVE(2, "待收货"),
    WAIT_COMMENT(3, "待评价"),
    FINISHED(4, "已完成"),
    CANCELLED(5, "已取消"),
    RETURNING(6, "退货申请中"),
    RETURNED(7, "已退货/退款");


    private final Integer code;
    private final String description;

    OrderStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据状态码获取对应的枚举实例
     * @param code 状态码
     * @return OrderStatus 枚举
     */
    public static OrderStatus getByCode(Integer code) {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null; // 或者根据业务抛出 IllegalArgumentException
    }
}
