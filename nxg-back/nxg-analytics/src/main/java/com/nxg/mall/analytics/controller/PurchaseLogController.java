package com.nxg.mall.analytics.controller;

import com.nxg.mall.analytics.entity.PurchaseLog;
import com.nxg.mall.analytics.service.IPurchaseLogService;
import com.nxg.mall.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/purchase")
public class PurchaseLogController {

    @Autowired
    private IPurchaseLogService purchaseLogService;

    @GetMapping("/list/{userId}")
    public Result<List<PurchaseLog>> getPurchaseLogs(@PathVariable Long userId) {
        List<PurchaseLog> list = purchaseLogService.getByUserId(userId);
        return Result.success(list);
    }
}
