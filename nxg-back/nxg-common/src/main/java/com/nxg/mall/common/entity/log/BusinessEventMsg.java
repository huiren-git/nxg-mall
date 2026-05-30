package com.nxg.mall.common.entity.log;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BusinessEventMsg {
    private String eventType; // REGISTER, PAYMENT, RECHARGE
    private Long userId;
    private Double amount;
    private Long timestamp;
}
