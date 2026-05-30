package com.nxg.mall.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体类
 * 对应表名：product (根据你的表名自行修改)
 */
@Data
@TableName("t_product")
public class Product {

    public static final Integer STATUS_ON = 1;
    public static final Integer STATUS_OFF = 0;

    /**
     * 商品 ID
     * IdType.AUTO 对应数据库的 AUTO_INCREMENT
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 所属类别 ID
     */
    private Long categoryId;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 商品成本价
     */
    private BigDecimal costPrice;


    /**
     * 商品单价
     */
    private BigDecimal price;

    /**
     * 状态：0-下架，1-上架, 2-逻辑删除
     */
    private Integer status;

    /**
     * 累计销量（件数）
     */
    private Integer salesCount;

    /**
     * 排序权重 ID（值越大越靠前）
     */
    private Integer sortOrder = 100;

    /**
     * 主图相对路径
     */
    private String mainImage;

    /**
     * 其他图片相对路径
     */
    private String subImages; // 逗号分隔的 URL

    /**
     * 逻辑删除标志
     * 并在 select 时自动过滤已删除的数据
     */
    @TableLogic
    private Boolean isDeleted;

    /**
     * 操作人id
     */
    private Long operatorId;

    /**
     * 上架时间
     * fill = FieldFill.INSERT 表示在插入数据时自动填充（需配合配置类）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 最后修改时间
     * fill = FieldFill.INSERT_UPDATE 表示插入和更新时自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 库存 之后放到SKU中
    private Integer stock;
}
