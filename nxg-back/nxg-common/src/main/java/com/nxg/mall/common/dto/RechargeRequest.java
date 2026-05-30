package com.nxg.mall.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RechargeRequest {

    /**
     * 充值金额
     * @NotNull: 确保金额不能为空
     * @DecimalMin: 确保金额必须大于 0.01（最小充值 1 分钱）
     */
    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于 0")
    private BigDecimal amount;

    /**
     * 支付渠道 (可选：1-支付宝, 2-微信, 3-人工后台)
     * 虽然你现在是直接提交，但预留这个字段方便后续扩展
     */
    private Integer payChannel;

    /**
     * 备注信息
     */
    private String remark;
}
