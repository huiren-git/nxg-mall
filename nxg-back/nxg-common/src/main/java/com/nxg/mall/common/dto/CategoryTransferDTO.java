package com.nxg.mall.common.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CategoryTransferDTO {
    @NotEmpty(message = "源分类不能为空")
    private List<Long> sourceCategoryIds;

    @NotNull(message = "目标分类不能为空")
    private Long targetCategoryId;
}
