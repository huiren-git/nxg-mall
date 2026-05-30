package com.nxg.mall.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 规格属性名实体 (例如：颜色、尺寸)
 */
@Data
public class AttributeKey {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联的模板id
     */
    private Long templateId;

    /**
     * 属性名称
     */
    private String attrName;

    /**
     * 逻辑删除 (0: 未删除, 1: 已删除)
     * 使用 @TableLogic 后，MP 的 delete 方法会自动转为 update，select 会自动过滤
     */
    @TableLogic
    private Integer isDeleted;

    /**
     * 创建时间
     * fill 属性可以配合 MyMetaObjectHandler 实现自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
