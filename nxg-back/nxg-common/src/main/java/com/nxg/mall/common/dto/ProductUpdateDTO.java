package com.nxg.mall.common.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductUpdateDTO {

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "名称长度不能超过100字")
    private String name;

    private List<Long> categoryPath;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotNull(message = "成本价不能为空")
    @DecimalMin(value = "0.01", message = "成本价必须大于0")
    private BigDecimal costPrice;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;

    private BigDecimal originalPrice;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    private String mainImage;

    private String subImages;

    private String description;

    /**
     * 商品关联的 SKU 列表
     */
    private List<SkuDTO> skuList;
}
