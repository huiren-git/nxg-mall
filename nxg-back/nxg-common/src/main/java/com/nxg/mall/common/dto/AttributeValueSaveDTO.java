package com.nxg.mall.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttributeValueSaveDTO {
    @NotNull(message = "属性名ID不能为空")
    private Long attrKeyId;

    @NotBlank(message = "属性值内容不能为空")
    private String attrValue;
}