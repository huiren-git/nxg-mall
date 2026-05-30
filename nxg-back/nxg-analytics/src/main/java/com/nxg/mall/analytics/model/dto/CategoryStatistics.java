package com.nxg.mall.analytics.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStatistics implements Serializable {
    private String categoryName;   // 类别名称
    private Double totalAmount;    // 总销售额
    private Integer totalCount;    // 总销售数量
    private String statsTime;      // 统计时间窗 (如: 2026-05-01 10:00:00)
}
