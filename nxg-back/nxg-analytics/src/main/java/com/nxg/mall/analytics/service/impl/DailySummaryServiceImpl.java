package com.nxg.mall.analytics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.analytics.entity.DailySummary;
import com.nxg.mall.analytics.mapper.DailySummaryMapper;
import com.nxg.mall.analytics.service.IDailySummaryService;
import com.nxg.mall.analytics.vo.TradeStatisticsVO;
import com.nxg.mall.analytics.vo.UserStatisticsVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DailySummaryServiceImpl extends ServiceImpl<DailySummaryMapper, DailySummary> implements IDailySummaryService {

    @Override
    public TradeStatisticsVO getTradeStatistics(LocalDate startDate, LocalDate endDate) {
        // 1. 查询指定日期范围内的数据，按日期升序
        LambdaQueryWrapper<DailySummary> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.between(DailySummary::getStatDate, startDate, endDate)
                .orderByAsc(DailySummary::getStatDate);
        List<DailySummary> dailyList = this.list(queryWrapper);

        TradeStatisticsVO vo = new TradeStatisticsVO();
        TradeStatisticsVO.SummaryData summary = new TradeStatisticsVO.SummaryData();
        TradeStatisticsVO.ChartData chart = new TradeStatisticsVO.ChartData();

        // 2. 初始化图表列表
        List<String> chartDays = new ArrayList<>();
        List<BigDecimal> revenueSeries = new ArrayList<>();
        List<BigDecimal> goodsSeries = new ArrayList<>();
        List<BigDecimal> conversionSeries = new ArrayList<>();
        List<BigDecimal> refundSeries = new ArrayList<>();
        List<BigDecimal> discountSeries = new ArrayList<>();

        // 3. 汇总累加变量
        BigDecimal totalRev = BigDecimal.ZERO;
        BigDecimal totalRecharge = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        int totalUv = 0;
        int totalTradeUsers = 0;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        // 4. 遍历处理数据
        for (DailySummary dayData : dailyList) {
            // 组装图表数据
            chartDays.add(dayData.getStatDate().format(formatter));

            BigDecimal dayRevenue = dayData.getTotalRevenue() != null ? dayData.getTotalRevenue() : BigDecimal.ZERO;
            BigDecimal dayRecharge = dayData.getRechargeAmount() != null ? dayData.getRechargeAmount() : BigDecimal.ZERO;
            BigDecimal dayGoodsAmount = dayRevenue.subtract(dayRecharge);

            revenueSeries.add(dayRevenue);
            goodsSeries.add(dayGoodsAmount);
            refundSeries.add(dayData.getRefundAmount() != null ? dayData.getRefundAmount() : BigDecimal.ZERO);
            discountSeries.add(dayData.getDiscountAmount() != null ? dayData.getDiscountAmount() : BigDecimal.ZERO);

            // 计算单日转化率 (交易人数 / UV) * 100
            BigDecimal dayRate = BigDecimal.ZERO;
            if (dayData.getUv() != null && dayData.getUv() > 0 && dayData.getTradeUsers() != null) {
                dayRate = new BigDecimal(dayData.getTradeUsers())
                        .divide(new BigDecimal(dayData.getUv()), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }
            conversionSeries.add(dayRate.setScale(1, RoundingMode.HALF_UP));

            // 累加汇总数据
            totalRev = totalRev.add(dayRevenue);
            totalRecharge = totalRecharge.add(dayRecharge);
            totalRefund = totalRefund.add(dayData.getRefundAmount() != null ? dayData.getRefundAmount() : BigDecimal.ZERO);
            totalDiscount = totalDiscount.add(dayData.getDiscountAmount() != null ? dayData.getDiscountAmount() : BigDecimal.ZERO);
            totalUv += (dayData.getUv() != null ? dayData.getUv() : 0);
            totalTradeUsers += (dayData.getTradeUsers() != null ? dayData.getTradeUsers() : 0);
        }

        // 5. 组装汇总数据 (Summary)
        summary.setTotalRevenue(totalRev);
        summary.setRechargeAmount(totalRecharge);
        summary.setGoodsAmount(totalRev.subtract(totalRecharge)); // 商品成交额 = 总营收 - 充值
        summary.setRefundAmount(totalRefund);
        summary.setDiscountAmount(totalDiscount);

        // 计算整体转化率
        BigDecimal overallRate = BigDecimal.ZERO;
        if (totalUv > 0) {
            overallRate = new BigDecimal(totalTradeUsers)
                    .divide(new BigDecimal(totalUv), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(1, RoundingMode.HALF_UP);
        }
        summary.setPayRate(overallRate);

        // 6. 赋值并返回
        chart.setChartDays(chartDays);
        chart.setRevenueSeries(revenueSeries);
        chart.setGoodsSeries(goodsSeries);
        chart.setConversionSeries(conversionSeries);
        chart.setRefundSeries(refundSeries);
        chart.setDiscountSeries(discountSeries);

        vo.setSummaryData(summary);
        vo.setChartData(chart);

        return vo;
    }

    @Override
    public UserStatisticsVO getUserStatistics(LocalDate startDate, LocalDate endDate) {
        // 1. 查询指定日期范围内的数据，并按时间升序排列
        LambdaQueryWrapper<DailySummary> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.between(DailySummary::getStatDate, startDate, endDate)
                .orderByAsc(DailySummary::getStatDate);
        List<DailySummary> dailyList = this.list(queryWrapper);

        // 2. 初始化 VO 对象
        UserStatisticsVO vo = new UserStatisticsVO();
        UserStatisticsVO.SummaryData summary = new UserStatisticsVO.SummaryData();
        UserStatisticsVO.ChartData chart = new UserStatisticsVO.ChartData();

        // 用于汇总聚合的变量
        int totalVisitors = 0, totalPageViews = 0, totalNewUsers = 0, totalTradeUsers = 0, totalRechargeUsers = 0;

        // 用于图表趋势的集合
        List<String> chartDays = new ArrayList<>();
        List<Integer> visitorsSeries = new ArrayList<>();
        List<Integer> newUsersSeries = new ArrayList<>();
        List<Integer> rechargeSeries = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        // 3. 遍历组装数据
        for (DailySummary dayData : dailyList) {
            // -- 处理图表趋势数组 --
            chartDays.add(dayData.getStatDate().format(formatter));

            int uv = dayData.getUv() != null ? dayData.getUv() : 0;
            int newUsers = dayData.getNewUsers() != null ? dayData.getNewUsers() : 0;
            int rechargeUsers = dayData.getRechargeUsers() != null ? dayData.getRechargeUsers() : 0;

            visitorsSeries.add(uv);
            newUsersSeries.add(newUsers);
            rechargeSeries.add(rechargeUsers);

            // -- 处理顶部卡片汇总累加 --
            totalVisitors += uv;
            totalPageViews += (dayData.getPv() != null ? dayData.getPv() : 0);
            totalNewUsers += newUsers;
            totalTradeUsers += (dayData.getTradeUsers() != null ? dayData.getTradeUsers() : 0);
            totalRechargeUsers += rechargeUsers;
        }

        // 4. 将汇总数据塞入 VO
        summary.setVisitors(totalVisitors);
        summary.setPageViews(totalPageViews);
        summary.setNewUsers(totalNewUsers);
        summary.setTradeUsers(totalTradeUsers);
        summary.setRechargeUsers(totalRechargeUsers);

        // 5. 将图表数据塞入 VO
        chart.setChartDays(chartDays);
        chart.setVisitorsSeries(visitorsSeries);
        chart.setNewUsersSeries(newUsersSeries);
        chart.setRechargeSeries(rechargeSeries);

        vo.setSummaryData(summary);
        vo.setChartData(chart);

        return vo;
    }
}
