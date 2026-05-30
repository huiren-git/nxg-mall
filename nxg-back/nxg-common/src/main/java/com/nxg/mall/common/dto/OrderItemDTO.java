package com.nxg.mall.common.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemDTO {
    private Long productId;
    private Long categoryId;
    private BigDecimal price;
    private Integer quantity;
}
