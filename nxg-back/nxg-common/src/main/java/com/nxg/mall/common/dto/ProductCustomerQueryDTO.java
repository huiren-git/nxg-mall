package com.nxg.mall.common.dto;

import lombok.Data;

@Data
public class ProductCustomerQueryDTO {
    private String keyword;        // 商品名称模糊查询 为空时表示所有商品
    private Long categoryId;    // 分类筛选
    private Integer pageNum = 1;
    private Integer pageSize = 10;

    private String sortField;
    private String sortOrder;
}
