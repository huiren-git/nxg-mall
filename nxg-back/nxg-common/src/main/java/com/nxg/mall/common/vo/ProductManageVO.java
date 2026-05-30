package com.nxg.mall.common.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductManageVO {
    private Long id;
    private String name;
    private BigDecimal price;
    private String mainImage;
    private Integer stock;        // 库存
    private Integer saleCount;
    private Boolean isOnShelf;    // 状态
    private LocalDateTime updateTime;
}
