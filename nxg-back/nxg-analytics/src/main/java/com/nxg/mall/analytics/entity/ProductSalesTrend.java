package com.nxg.mall.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_product_sales_trend")
public class ProductSalesTrend {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String periodName;
    private Integer salesVolume;
    private Integer isForecast; // 0-历史，1-预测
    private Integer sortOrder;
}
