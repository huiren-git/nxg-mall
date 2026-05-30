package com.nxg.mall.common.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSearchVO {
    private Long id;
    private String name;
    private Long categoryId;
    private String description;
    private BigDecimal price;
    private Integer salesCount;
    private String mainImage;
    private Integer stock;
}
