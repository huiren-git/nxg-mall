package com.nxg.mall.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Sku {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属商品(SPU) ID
     */
    private Long productId;

    /**
     * SKU 唯一编码 (用于扫码、订单唯一标识)
     */
    private String skuCode;

    /**
     * 规格属性，建议存储 JSON 字符串
     * 示例：{"color": "星空灰", "memory": "16G"}
     */
    private String specs;

    /**
     * 该规格对应的具体价格 (会覆盖 Product 表的 price)
     */
    private BigDecimal price;

    /**
     * 对应规格的库存
     */
    private Integer stock;

    /**
     * 该规格特有的图片（例如点击不同颜色切换图片）
     */
    private String mainImage;

    /**
     * 状态：0-下架，1-上架
     */
    private Integer status;

    /**
     * 逻辑删除标志
     */
    @TableLogic
    private Boolean isDeleted;

    /**
     * 操作人id
     */
    private Long operatorId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 最后修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 辅助方法：获取图片完整路径 (同 Product 逻辑)
     */
    @TableField(exist = false)
    private String fullImageUrl;
}
