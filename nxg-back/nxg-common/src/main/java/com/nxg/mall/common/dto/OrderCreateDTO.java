package com.nxg.mall.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.nxg.mall.common.vo.OrderItemVO;

import java.util.List;

@Data
public class OrderCreateDTO {
    @NotNull(message = "请选择收货地址")
    private Long addressId;

    private String remark; // 订单备注（选填）

    // 优惠券ID（预留）
    private List<Long> couponIds;

    // 支付方式扩充
    // private Integer payType;     // 支付方式 (1-支付宝, 2-微信)
}
