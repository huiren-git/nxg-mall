package com.nxg.mall.analytics.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductRankVO {
    private Integer rank;        // 排名
    private String id;           // 对应前端 record.id
    private String name;         // 商品名称
    private String category;     // 类目
    private Integer sales;       // 销量
    private BigDecimal revenue;  // 销售额
    private Integer trend;       // 趋势(百分比)
    private String image;        // 图片地址
}