package com.nxg.mall.common.dto.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRefundMessageDTO {
    private String orderNo;
    private Long userId;
    private BigDecimal refundAmount; // 退款金额
    private LocalDateTime refundTime; // 退款时间
}
