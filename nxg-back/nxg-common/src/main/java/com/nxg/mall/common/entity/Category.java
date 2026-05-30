package com.nxg.mall.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 商品类别实体类
 */
@Data
public class Category implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 类别名称
     */
    private String name;

    /**
     * 父类别 ID，0 表示根节点
     */
    private Long parentId;

    /**
     * 层级 (1, 2, 3)
     */
    private Integer level;

    /**
     * 路径: /父id/子id
     */
    @NotBlank
    private String path;

    /**
     * 同一层级下的显示顺序
     */
    private Integer sortOrder;

    // 逻辑删除标记 0-未删除 1-逻辑删除
    @TableLogic
    private Integer is_deleted;

    /**
     * 创建该类别的销售id (可为 NULL)
     */
    private Long operatorId;
}