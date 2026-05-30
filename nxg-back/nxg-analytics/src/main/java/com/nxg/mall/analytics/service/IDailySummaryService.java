package com.nxg.mall.analytics.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.analytics.entity.DailySummary;
import com.nxg.mall.analytics.vo.TradeStatisticsVO;
import com.nxg.mall.analytics.vo.UserStatisticsVO;

import java.time.LocalDate;

public interface IDailySummaryService extends IService<DailySummary> {

    /**
     * 获取交易统计报表数据
     */
    TradeStatisticsVO getTradeStatistics(LocalDate startDate, LocalDate endDate);

    /**
     * 获取用户统计报表数据
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 统计报表VO
     */
    UserStatisticsVO getUserStatistics(LocalDate startDate, LocalDate endDate);
}
