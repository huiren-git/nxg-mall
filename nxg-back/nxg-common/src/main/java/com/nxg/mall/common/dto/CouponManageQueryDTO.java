package com.nxg.mall.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CouponManageQueryDTO implements Serializable {

    // 分页参数设置默认值
    private Integer current = 1;
    private Integer size = 10;

    // 业务查询过滤
    private Integer status;
    private String title;
    private Integer isDeleted = 0;
    private Integer category;

    // 以后扩展很方便，比如：
    // private BigDecimal minConditionAmount;
    // private String startTime;
}