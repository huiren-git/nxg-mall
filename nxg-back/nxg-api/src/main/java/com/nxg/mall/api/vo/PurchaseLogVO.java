package com.nxg.mall.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseLogVO {
    private Long id;              // 订单明细表(t_order_item)的id
    private String orderNo;       // 订单号
    private Long userId;          // 用户id
    private String productName;   // 商品名称
    private String productImage;  // 商品图片
    private String category;      // 商品分类名称
    private BigDecimal unitPrice; // 购买单价
    private Integer quantity;     // 购买数量
    private Integer status;       // 订单状态（用于前端的状态标签判定）

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime purchaseTime; // 购买时间（即订单的付款时间 pay_time）
}
