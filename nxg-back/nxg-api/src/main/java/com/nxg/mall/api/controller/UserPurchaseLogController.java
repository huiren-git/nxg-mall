package com.nxg.mall.api.controller;

import com.nxg.mall.api.service.IOrderService;
import com.nxg.mall.api.vo.PurchaseLogVO;
import com.nxg.mall.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/logs")
public class UserPurchaseLogController {

    @Autowired
    private IOrderService orderService;

    @GetMapping("/purchase-list")
    // 💡 由销售人员显式显式传入要查询的客户 userId
    public Result<List<PurchaseLogVO>> getPurchaseLogList(@RequestParam Long userId) {
        List<PurchaseLogVO> list = orderService.getPurchaseLogs(userId);
        return Result.success(list);
    }
}
