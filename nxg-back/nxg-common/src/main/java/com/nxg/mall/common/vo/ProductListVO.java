package com.nxg.mall.common.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductListVO {
    private Long id;
    private String name;
    private Long categoryId;
    private BigDecimal price;
    private String mainImage;
    private Integer salesCount;
    private Boolean hasStock;
}
