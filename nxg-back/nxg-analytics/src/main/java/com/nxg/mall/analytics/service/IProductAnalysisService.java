package com.nxg.mall.analytics.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.analytics.entity.ProductAnalysis;
import com.nxg.mall.analytics.vo.ProductAnalysisVO;
import com.nxg.mall.analytics.vo.ProductDashboardVO;
import com.nxg.mall.analytics.vo.ProductSalesPivotVO;

import java.time.LocalDate;

public interface IProductAnalysisService extends IService<ProductAnalysis> {
    ProductDashboardVO getDashboardData(LocalDate startDate, LocalDate endDate);

    IPage<ProductAnalysisVO> getProductPage(int current, int size, LocalDate startDate, LocalDate endDate, String category, String status);

    ProductSalesPivotVO getProductPivotDetails(Long productId);
}
