package com.nxg.mall.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor  // 重要：MyBatis 反射需要
@AllArgsConstructor // 方便调试
public class OrderVO {
    private Long id;
    private String orderNo;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private Integer payType;       // 支付类型：1-余额，2-支付宝，3-微信，4-混合支付
    private Integer status; // 0-待支付, 1-已支付...

    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // 包含订单下的商品明细
    private List<OrderItemVO> orderItems;
}
