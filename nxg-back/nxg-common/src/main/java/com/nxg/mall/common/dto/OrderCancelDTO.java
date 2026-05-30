package com.nxg.mall.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderCancelDTO {
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotBlank(message = "取消原因不能为空")
    private String reason;
}
