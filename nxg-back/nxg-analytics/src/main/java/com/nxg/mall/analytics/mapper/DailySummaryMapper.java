package com.nxg.mall.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nxg.mall.analytics.entity.DailySummary;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

@Mapper
public interface DailySummaryMapper extends BaseMapper<DailySummary> {

    /**
     * 根据指定日期，聚合各个 DWD 明细表的数据并更新到 t_daily_summary 中
     * @param targetDate 目标日期，格式需为 "yyyy-MM-dd"，例如 "2026-05-17"
     * @return 影响的行数
     */
    int calculateAndSaveDailySummary(@Param("targetDate") String targetDate);
}
