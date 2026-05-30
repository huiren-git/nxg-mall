package com.nxg.mall.common.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderCouponCheckDTO {
    private List<OrderItemDTO> items; // 当前订单选中的商品列表
}
