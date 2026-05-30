package com.nxg.mall.api.vo;

import com.nxg.mall.common.vo.SkuVO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductDetailManageVO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal costPrice;
    private BigDecimal price; // 展示起步价或 SPU 价格
    private String mainImage;
    private String subImages;
    private Integer stock; // 冗余显示总库存
    private Long categoryId; // 基础 ID
    private List<Long> categoryPath; // 仅在详情接口递归生成

    // SKU 列表
    private List<SkuVO> skuList;
}
