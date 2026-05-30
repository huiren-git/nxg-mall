package com.nxg.mall.analytics.controller;

import com.nxg.mall.common.Result;
import com.nxg.mall.analytics.service.IAdsSalesForecastService;
import com.nxg.mall.analytics.vo.SalesForecastVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/forecast")
public class SalesForecastController {

    @Autowired
    private IAdsSalesForecastService forecastService;

    /**
     * 获取销售预测看板数据
     * GET /api/admin/forecast/sales?model=prophet&category=all
     */
    @GetMapping("/sales")
    public Result<SalesForecastVO> getSalesForecast(
            @RequestParam(defaultValue = "prophet") String model,
            @RequestParam(defaultValue = "all") String category) {

        SalesForecastVO data = forecastService.getSalesForecastDashboard(model, category);
        return Result.success(data);
    }
}
