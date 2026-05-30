package com.nxg.mall.common.entity;

import lombok.Data;

import com.baomidou.mybatisplus.annotation.*;

/**
 * 规格属性值实体 (例如：黑色、白色、256GB)
 */
@Data
public class AttributeValue {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联的属性名 ID (t_attribute_key 表 id)
     */
    private Long attrKeyId;

    /**
     * 具体属性值
     */
    private String attrValue;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer isDeleted;
}
