package com.nxg.mall.common.dto;

import lombok.Data;

@Data
public class CartDTO {
    private Long productId;
    private Long skuId;
    private Integer quantity;
}