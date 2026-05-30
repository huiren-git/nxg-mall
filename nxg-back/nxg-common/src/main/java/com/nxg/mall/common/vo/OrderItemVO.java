package com.nxg.mall.common.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor  // 重要：MyBatis 反射需要
@AllArgsConstructor // 方便调试
public class OrderItemVO {
    private Long id;
    private Long productId;
    private Long skuId;
    private String specs;
    private String productName;
    private String productPic;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal payAmount; // 该项实付
}
