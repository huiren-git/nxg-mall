package com.nxg.mall.common.dto;

import lombok.Data;

@Data
public class CartUpdateDTO {
    private Long productId;
    private Long skuId;
    private Integer count;    // 对应数量
    private Integer checked;  // 对应选中状态 (0或1)
}
