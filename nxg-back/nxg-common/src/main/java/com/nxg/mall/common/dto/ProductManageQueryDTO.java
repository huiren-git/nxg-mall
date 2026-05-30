package com.nxg.mall.common.dto;

import lombok.Data;

@Data
public class ProductManageQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;

    /**
     * 业务状态：0-已下架，1-销售中
     */
    private Integer status;

    /**
     * 是否查询已删除商品（回收站）
     */
    private Boolean queryDeleted = false;

    private String keyword;
}
