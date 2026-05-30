package com.nxg.mall.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import com.nxg.mall.common.dto.PaymentItemDTO;

@Data
public class PayDTO {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /**
     * 支付方式类型
     * 1: 余额支付 (balance)
     * 2: 微信支付 (wechat) - TODO
     * 3: 支付宝支付 (alipay) - TODO
     */
    @NotNull(message = "请选择支付方式")
    private List<PaymentItemDTO> payments;

    // 如果后续涉及优惠券或积分抵扣，可以在此扩展字段
    // private Long couponId;
}
