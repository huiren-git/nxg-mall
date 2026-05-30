package com.nxg.mall.common.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BrowseLogVO implements Serializable {
    private Long id;            // 浏览记录ID
    private Long productId;     // 商品ID
    private String productName; // 商品名称
    private String productPic;  // 商品主图
    private BigDecimal price;   // 商品价格
    private LocalDateTime enterTime; // 浏览时间
}
