package com.nxg.mall.common.dto;

import lombok.Data;

@Data
public class ProductQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String sortField; // sales, price, time
    private String sortOrder; // asc, desc
}
