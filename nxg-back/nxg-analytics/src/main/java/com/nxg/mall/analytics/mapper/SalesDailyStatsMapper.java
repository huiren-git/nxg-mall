package com.nxg.mall.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nxg.mall.analytics.entity.SalesDailyStats;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SalesDailyStatsMapper extends BaseMapper<SalesDailyStats> {
}

// 对应的 HourlyTrendMapper 类似
