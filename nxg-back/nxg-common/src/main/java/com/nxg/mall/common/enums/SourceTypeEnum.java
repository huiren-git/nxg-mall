package com.nxg.mall.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SourceTypeEnum {
    DIRECT("DIRECT", "直接访问"),
    SEARCH("SEARCH", "搜索结果"),
    RECOMMEND("RECOMMEND", "首页推荐"),
    CATEGORY_LIST("CATEGORY_LIST", "商品类别商品列表"),
    COUPON_LIST("COUPON_LIST", "优惠券适用商品页");

    @EnumValue // 告诉 MyBatis：数据库存的是这个 code 字段的值
    private final String code;

    @JsonValue // 告诉 Jackson：返回给前端的是这个 description 字段的值
    private final String description;
}