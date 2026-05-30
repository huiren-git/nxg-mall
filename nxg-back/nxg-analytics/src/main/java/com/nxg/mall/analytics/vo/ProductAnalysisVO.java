package com.nxg.mall.analytics.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductAnalysisVO {
    private Long id;
    private String name;          // 对应前端 columns: name，解耦属性名
    private String category;
    private Integer stock;
    private String status;
    private Integer salesVolume;
    private BigDecimal turnover;  // 对应前端 columns: turnoverRate
}
