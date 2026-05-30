package com.nxg.mall.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_product_metric_detail")
public class ProductMetricDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String metricDim;
    private String currentValue;
    private String momChange;
    private String diagnosticResult;
}
