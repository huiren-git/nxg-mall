package com.nxg.mall.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Cart {

    /**
     * 自增主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * SKUID
     */
    private Long skuId;
    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 是否选中：0-未选中，1-选中
     * 默认值建议在数据库层面设为 1
     */
    private Integer isChecked;

    /**
     * 创建时间
     * fill = FieldFill.INSERT 表示插入时自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     * fill = FieldFill.INSERT_UPDATE 表示插入和更新时自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
