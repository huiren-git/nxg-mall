package com.nxg.mall.analytics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.analytics.entity.AdsSalesForecast;
import com.nxg.mall.analytics.mapper.AdsSalesForecastMapper;
import com.nxg.mall.analytics.service.IAdsSalesForecastService;
import com.nxg.mall.analytics.vo.SalesForecastVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Service
public class AdsSalesForecastServiceImpl extends ServiceImpl<AdsSalesForecastMapper, AdsSalesForecast> implements IAdsSalesForecastService {

    @Override
    public SalesForecastVO getSalesForecastDashboard(String modelName, String category) {
        SalesForecastVO vo = new SalesForecastVO();

        // 1. 获取当前预测月份 (自动动态计算下个月，例如当前是2026年5月，则计算出 "2026-06")
        String targetMonth = LocalDate.now().plusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));

// 2. 从数据库查出对应的宏观预测指标
        LambdaQueryWrapper<AdsSalesForecast> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AdsSalesForecast::getModelName, modelName)
                // ======= ✨ 核心修改：将原本写死的 "2026-05" 替换为动态变量 targetMonth =======
                .eq(AdsSalesForecast::getForecastMonth, targetMonth)
                // =======================================================================
                .last("LIMIT 1");

        AdsSalesForecast dbRecord = this.getOne(queryWrapper);

        // 3. 组装顶部宏观指标 (Summary)
        SalesForecastVO.SummaryMetrics summary = new SalesForecastVO.SummaryMetrics();
        if (dbRecord != null) {
            summary.setExpectedRevenue(dbRecord.getExpectedRevenue());
            summary.setGrowthRate(dbRecord.getExpectedGrowthRate());
            summary.setSeasonalityCoefficient(dbRecord.getSeasonalityCoefficient());
            // 数据库没有准确率字段，根据模型 mock 一下
            summary.setAccuracy(modelName.equals("prophet") ? new BigDecimal("92.45") : new BigDecimal("89.10"));
        } else {
            // 兜底数据
            summary.setExpectedRevenue(BigDecimal.ZERO);
            summary.setGrowthRate(BigDecimal.ZERO);
            summary.setSeasonalityCoefficient(BigDecimal.ONE);
            summary.setAccuracy(BigDecimal.ZERO);
        }
        vo.setSummary(summary);

        return vo;
    }
}
