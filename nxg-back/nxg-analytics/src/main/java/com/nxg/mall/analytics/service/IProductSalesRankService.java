package com.nxg.mall.analytics.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.analytics.entity.ProductSalesRank;
import com.nxg.mall.analytics.vo.ProductRankOverviewVO;
import com.nxg.mall.analytics.vo.ProductRankVO;
import java.util.List;

public interface IProductSalesRankService extends IService<ProductSalesRank> {
    /**
     * 根据时间范围获取排行榜
     * @param timeRange 'today', '7d', '30d'
     */
    ProductRankOverviewVO getLeaderboard(String timeRange);
}
