package com.nxg.mall.api.service;

import java.math.BigDecimal;

public interface BalanceService {
    /**
     * 发起充值
     * @return 返回生成的充值订单号
     */
    String recharge(Long userId, BigDecimal amount);
}
