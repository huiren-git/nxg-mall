package com.nxg.mall.analytics.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BusinessTypeEnum {
    ORDER(1, "订单域"),
    PAY(2, "支付域"),
    STOCK(3, "库存域"),
    USER(4, "用户域");

    @EnumValue
    private final int code;

    @JsonValue
    private final String desc;
}