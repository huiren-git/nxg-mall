package com.nxg.mall.analytics.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_product_sales_rank")
public class ProductSalesRank {
    @TableId
    private Long productId;
    private String productName;
    private String category;
    private Integer salesVolume;
    private BigDecimal salesAmount;
    private LocalDateTime updateTime;
    private Integer productPv;
    private Integer orderCount;
    private String mainImage;
}
