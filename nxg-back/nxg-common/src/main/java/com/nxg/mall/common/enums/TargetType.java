package com.nxg.mall.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TargetType {
    ORDER("ORDER", "订单"),
    PRODUCT("PRODUCT", "商品"),
    CATEGORY("CATEGORY","商品类别"),
    COUPON("COUPON", "优惠券"),
    ATTRIBUTE("ATTRIBUTE", "商品属性"),
    USER("USER", "用户"),
    SALES("SALES", "销售人员"),
    SYSTEM("SYSTEM", "系统配置"),
    ANALYTICS("ANALYTICS", "统计数据");

    @EnumValue
    private final String code;

    @JsonValue
    private final String description;

    /**
     * 根据 code 获取描述，用于日志转换
     */
    public static String getDescriptionByCode(String code) {
        for (TargetType type : TargetType.values()) {
            if (type.code.equals(code)) {
                return type.description;
            }
        }
        return "未知对象";
    }
}
