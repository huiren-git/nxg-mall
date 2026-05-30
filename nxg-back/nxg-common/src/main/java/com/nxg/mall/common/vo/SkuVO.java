package com.nxg.mall.common.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuVO {
    private Long id;
    private String skuCode;
    private String specs; // 建议前端解析 JSON 字符串，或后端转为 Map<String, String>
    private BigDecimal price;
    private Integer stock;
    private String mainImage;
}
