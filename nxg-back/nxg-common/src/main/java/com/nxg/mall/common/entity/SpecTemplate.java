package com.nxg.mall.common.entity;

import lombok.Data;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 规格模板主表 (t_spec_template)
 */
@Data
@TableName("t_spec_template")
public class SpecTemplate {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板名称 (如：夏季服饰规格模板)
     */
    private String templateName;

    /**
     * 关联分类ID (0代表全系统通用)
     */
    private Long categoryId;

    /**
     * 创建时间
     * fill = FieldFill.INSERT 表示在插入时自动填充（需配置MyBatis-Plus拦截器）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除 (0: 未删除, 1: 已删除)
     * 使用 @TableLogic 后，调用 deleteById 等方法会执行 UPDATE 操作
     */
    @TableLogic
    private Integer isDeleted;
}
