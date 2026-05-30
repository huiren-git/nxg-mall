package com.nxg.mall.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ActionType {
    // 订单相关
    CREATE_ORDER("ORDER_CREATE", "创建订单"),
    DELIVER("DELIVER", "确认发货"),
    CANCEL_ORDER("CANCEL", "取消订单"),
    MODIFY_PRICE("MODIFY_PRICE", "修改价格"),
    REFUND_APPROVE("REFUND_APPROVE", "退款审核通过"),
    REFUND_REJECT("REFUND_REJECT", "退款审核驳回"),

    // 商品相关
    ADD_PRODUCT("ADD", "上架商品"),
    UPDATE_PRODUCT("UPDATE", "更新商品信息"),
    DELETE_PRODUCT("DELETE", "下架商品"),

    // 商品分类相关
    ADD_CATEGORY("ADD_CATEGORY", "新增商品分类"),
    UPDATE_CATEGORY("UPDATE_CATEGORY", "更新商品分类"),
    DELETE_CATEGORY("DELETE_CATEGORY", "删除商品分类"),

    // 商品属性相关
    ADD_ATTRIBUTE("ADD_ATTRIBUTE", "新增属性"),
    UPDATE_ATTRIBUTE("UPDATE_ATTRIBUTE", "修改属性"),
    DELETE_ATTRIBUTE("DELETE_ATTRIBUTE", "删除属性"),

    // 优惠券相关
    ADD_COUPON("ADD_COUPON", "创建优惠券"),
    UPDATE_COUPON("UPDATE_COUPON", "修改优惠券"),
    DELETE_COUPON("DELETE_COUPON", "删除优惠券"),

    // 用户/安全相关
    BAN_USER("BAN", "禁用用户"),
    UNBAN_USER("UNBAN", "解封用户"),
    RESET_PWD("RESET_PWD", "重置密码"),
    ADD_SALES("ADD_SALES","新增销售人员"),
    UPDATE_SALES("UPDATE_SALES","更新销售人员信息"),
    DELETE_SALES("DELETE_SALES","删除销售人员"),

    // 系统相关
    CONFIG_UPDATE("CONFIG_UPDATE", "修改系统配置");

    @EnumValue
    private final String code;

    @JsonValue
    private final String description;
}
