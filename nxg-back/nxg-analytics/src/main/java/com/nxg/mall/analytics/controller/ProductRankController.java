package com.nxg.mall.analytics.controller;

import com.nxg.mall.analytics.vo.ProductRankOverviewVO;
import com.nxg.mall.common.Result; // 假设你有统一返回类
import com.nxg.mall.analytics.service.IProductSalesRankService;
import com.nxg.mall.analytics.vo.ProductRankVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/rank")
public class ProductRankController {

    @Autowired
    private IProductSalesRankService rankService;

    /**
     * 获取商品销售排行榜
     * GET /api/admin/rank/leaderboard?timeRange=7d
     */
    @GetMapping("/leaderboard")
    public Result<ProductRankOverviewVO> getLeaderboard(
            @RequestParam(defaultValue = "7d") String timeRange) {

        ProductRankOverviewVO data = rankService.getLeaderboard(timeRange);
        return Result.success(data);
    }
}
