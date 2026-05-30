package com.nxg.mall.common.vo;

import lombok.Data;

import java.math.BigDecimal;

import java.util.List;

@Data
public class UserCenterVO {
    // 1. 基本信息
    private String nickname;
    private String avatar;
    private BigDecimal balance; // 账户余额
    private String email;

    // 2. 统计数据
    private Integer cartCount;      // 购物车商品数量
    private Integer couponCount;    // 优惠券数量
    private String browsingHistory; // TODO: 浏览历史占位

    // 3. 最近订单列表 (OrderItem 相关)
    private List<UserOrderItemVO> recentOrders;
}
