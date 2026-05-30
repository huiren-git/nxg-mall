package com.nxg.mall.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductCreateDTO {
    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String mainImage;
    private String subImages;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    private String description;

    @NotNull(message = "成本价不能为空")
    private BigDecimal costPrice;

    @NotNull(message = "展示价格不能为空")
    private BigDecimal price;
    private Integer status;
    private Integer sortOrder;

    @NotEmpty(message = "至少需要包含一个SKU规格")
    @Valid
    private List<SkuDTO> skuList;
}
