package com.nxg.mall.common.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuDTO {
    /**
     * SKU ID，如果是前端新增的 SKU，这个字段为空；如果是修改已有的，这个字段有值
     */
    private Long id;

    /**
     * 规格属性字符串 (前端传来的 JSON)
     */
    private String specs;

    private BigDecimal price;
    private Integer stock;
    private String mainImage;
}
