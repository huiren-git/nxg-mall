package com.nxg.mall.common.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentItemDTO {
    /**
     * 支付类型：1-余额, 2-支付宝, 3-微信, 4-积分抵扣
     */
    private Integer type;

    /**
     * 该支付方式对应的金额
     */
    private BigDecimal amount;

    /**
     * 备注信息（可选，例如：某次支付使用的卡号末四位或优惠券ID）
     */
    private String remark;
}