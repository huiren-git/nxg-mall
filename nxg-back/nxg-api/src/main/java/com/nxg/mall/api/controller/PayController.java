package com.nxg.mall.api.controller;

import com.nxg.mall.api.service.BalanceService;
import com.nxg.mall.common.Result;
import com.nxg.mall.common.dto.PayDTO;
import com.nxg.mall.api.service.PayService;
import com.nxg.mall.common.dto.RechargeRequest;
import com.nxg.mall.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pay")
public class PayController {

    @Autowired
    private PayService payService;

    @Autowired
    private BalanceService balanceService;

    @PostMapping("/confirm")
    public Result<Void> confirmPay(@RequestBody PayDTO payDTO) {
        // 调用 Service 执行支付逻辑
        payService.processPay(payDTO);
        return Result.success();
    }

    @PostMapping("/recharge")
    public Result<String> recharge(@RequestBody RechargeRequest request) {
        // 从 Security 上下文获取当前登录用户 ID
        Long userId = SecurityUtils.getUserId();
        String orderNo = balanceService.recharge(userId, request.getAmount());
        return Result.success(orderNo);
    }
}
