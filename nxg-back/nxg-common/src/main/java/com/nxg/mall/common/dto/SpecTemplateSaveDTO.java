package com.nxg.mall.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SpecTemplateSaveDTO {
    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    // 前端传来的规格列表
    @NotEmpty(message = "规格配置不能为空")
    private List<SpecItemDTO> specList;

    @Data
    public static class SpecItemDTO {
        @NotBlank(message = "规格名不能为空")
        private String attrName; // 如：“颜色”

        @NotBlank(message = "规格值不能为空")
        private String attrValues; // 前端传的是逗号分隔字符串，如：“红色,白色”
    }
}
