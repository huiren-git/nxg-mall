package com.nxg.mall.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderDeliveryDTO {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;
}
