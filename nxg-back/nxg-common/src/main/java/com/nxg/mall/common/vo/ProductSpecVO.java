package com.nxg.mall.common.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ProductSpecVO {
    private List<SpecKeyVO> specKeys; // 规格属性及其可选值
    private Map<String, SkuInfoVO> skuMap; // 规格路径映射表

    @Data
    public static class SpecKeyVO {
        private Long id;
        private String name;
        private List<String> values;
    }

    @Data
    public static class SkuInfoVO {
        private Long skuId;
        private String skuCode;
        private BigDecimal price;
        private Integer stock;
        private String mainImage;
    }
}
