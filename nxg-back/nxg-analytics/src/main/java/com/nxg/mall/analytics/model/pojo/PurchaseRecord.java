package com.nxg.mall.analytics.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRecord implements Serializable {
    private String categoryName;
    private Double price;
    private Integer quantity;
    private Long timestamp; // 购买时间戳
}
