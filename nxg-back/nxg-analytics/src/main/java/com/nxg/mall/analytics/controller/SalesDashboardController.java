package com.nxg.mall.analytics.controller;

import com.nxg.mall.analytics.service.ISalesDashboardService;
import com.nxg.mall.analytics.vo.SalesTradeDashboardVO;
import com.nxg.mall.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sales")
public class SalesDashboardController {

    @Autowired
    private ISalesDashboardService salesDashboardService;

    @GetMapping("/trade-dashboard")
    public Result<SalesTradeDashboardVO> getDashboard() {
        return Result.success(salesDashboardService.getFullDashboardData());
    }
}
