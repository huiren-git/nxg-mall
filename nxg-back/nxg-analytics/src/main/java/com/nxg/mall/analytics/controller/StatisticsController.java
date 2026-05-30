package com.nxg.mall.analytics.controller;

import com.nxg.mall.analytics.service.IDailySummaryService;
import com.nxg.mall.analytics.vo.TradeStatisticsVO;
import com.nxg.mall.analytics.vo.UserStatisticsVO;
import com.nxg.mall.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/statistics")
public class StatisticsController {

    @Autowired
    private IDailySummaryService dailySummaryService;

    /**
     * 获取交易统计报表
     * GET /api/admin/statistics/trade?startDate=2026-05-01&endDate=2026-05-07
     */
    @GetMapping("/trade")
    public Result<TradeStatisticsVO> getTradeStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        // 获取报表数据
        TradeStatisticsVO data = dailySummaryService.getTradeStatistics(startDate, endDate);

        // 使用统一的 Result 类进行包装并返回
        return Result.success(data);
    }

    /**
     * 获取用户统计报表
     * GET /api/admin/statistics/user?startDate=2026-05-01&endDate=2026-05-07
     */
    @GetMapping("/user")
    public Result<UserStatisticsVO> getUserStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        // 调用 Service 获取拼装好的报表数据
        UserStatisticsVO data = dailySummaryService.getUserStatistics(startDate, endDate);

        // 使用你的通用返回类包装结果
        return Result.success(data);
    }

}
