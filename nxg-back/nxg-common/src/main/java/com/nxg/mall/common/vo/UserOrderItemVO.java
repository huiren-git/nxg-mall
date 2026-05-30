package com.nxg.mall.common.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserOrderItemVO {
    private String orderNo;
    private String productName;
    private String productPic;
    private Integer quantity;
    private BigDecimal payAmount;
    private Integer status;        // 订单状态
    private LocalDateTime createTime;
}
