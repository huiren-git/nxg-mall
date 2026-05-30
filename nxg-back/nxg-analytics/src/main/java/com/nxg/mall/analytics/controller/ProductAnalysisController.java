package com.nxg.mall.analytics.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nxg.mall.analytics.entity.ProductAnalysis;
import com.nxg.mall.analytics.service.IProductAnalysisService;
import com.nxg.mall.analytics.vo.ProductAnalysisVO;
import com.nxg.mall.analytics.vo.ProductDashboardVO;
import com.nxg.mall.analytics.vo.ProductSalesPivotVO;
import com.nxg.mall.common.Result;
import com.nxg.mall.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping()
public class ProductAnalysisController {

    @Autowired
    private IProductAnalysisService analysisService;
    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/admin/product-analysis/dashboard")
    public Result<ProductDashboardVO> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(pattern = "YYYY-MM-DD") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "YYYY-MM-DD") LocalDate endDate) {
        return Result.success(analysisService.getDashboardData(startDate, endDate));
    }

    /**
     * 分页获取商品明细（含过滤与时间范围筛选）
     */
    @GetMapping("/admin/product-analysis/list")
    public Result<IPage<ProductAnalysisVO>> getList(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(pattern = "YYYY-MM-DD") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "YYYY-MM-DD") LocalDate endDate,
            String category,
            String status) {
        return Result.success(analysisService.getProductPage(current, size, startDate, endDate, category, status));
    }

    /**
     * 获取指定商品的透视分析大包数据
     */
    @GetMapping("/sales/pivot/{productId}")
    public Result<ProductSalesPivotVO> getProductPivot(@PathVariable Long productId) {
        ProductSalesPivotVO pivotData = analysisService.getProductPivotDetails(productId);
        if (pivotData == null) {
            throw new BusinessException("未查询到对应商品的数据");
        }
        return Result.success(pivotData);
    }
}
