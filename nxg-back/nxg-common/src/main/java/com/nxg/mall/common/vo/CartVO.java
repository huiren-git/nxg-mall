package com.nxg.mall.common.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartVO {
    private Long id;
    private Long productId;
    private Long skuId;
    private String productName;

    /**
     * SKU 规格描述，例如：{"颜色": "曜石黑", "内存": "12GB+256GB"}
     */
    private String specs;

    private String productMainImage; // SPU 主图
    private String skuImageUrl;      // SKU 特有图（如果有）

    private BigDecimal price;    // 注意：这里必须是 SKU 的单价
    private Integer quantity;    // 购买数量
    private Integer isChecked;   // 是否选中
    private BigDecimal subTotal; // 小计 (SKU单价 * quantity)

    private Integer stock;       // 冗余库存字段，方便前端判断是否还能继续加购
}
