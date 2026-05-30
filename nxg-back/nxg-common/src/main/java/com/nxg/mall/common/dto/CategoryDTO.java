package com.nxg.mall.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


// 用于Sales创建/修改类目
@Data
public class CategoryDTO {

    @NotBlank(message = "分类名称不能为空")
    private String name;

    @NotNull(message = "上级分类不能为空")
    private Long parentId = 0L;

    private Integer sortOrder = 0;
}
