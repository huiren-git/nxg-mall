package com.nxg.mall.analytics.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nxg.mall.analytics.entity.SalesDailyStats;
import com.nxg.mall.analytics.entity.SalesHourlyTrend;
import com.nxg.mall.analytics.entity.SystemAnomalyQueue;
import com.nxg.mall.analytics.mapper.SalesDailyStatsMapper;
import com.nxg.mall.analytics.mapper.SalesHourlyTrendMapper;
import com.nxg.mall.analytics.mapper.SystemAnomalyQueueMapper;
import com.nxg.mall.analytics.service.ISalesDashboardService;
import com.nxg.mall.analytics.vo.SalesTradeDashboardVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SalesDashboardServiceImpl implements ISalesDashboardService {

    @Autowired
    private SalesDailyStatsMapper dailyStatsMapper;
    @Autowired
    private SalesHourlyTrendMapper hourlyTrendMapper;
    @Autowired
    private SystemAnomalyQueueMapper anomalyQueueMapper;

    @Override
    public SalesTradeDashboardVO getFullDashboardData() {
        SalesTradeDashboardVO vo = new SalesTradeDashboardVO();
        LocalDate today = LocalDate.now();

        // 1. 获取今日汇总指标
        SalesDailyStats daily = dailyStatsMapper.selectOne(
                Wrappers.<SalesDailyStats>lambdaQuery().eq(SalesDailyStats::getStatDate, today)
        );

        if (daily != null) {
            vo.setStats(Arrays.asList(
                    new SalesTradeDashboardVO.StatCard("今日销售额", "¥" + daily.getTotalSalesAmount(), daily.getSalesTrend(), "#3f8600"),
                    new SalesTradeDashboardVO.StatCard("今日订单数", String.valueOf(daily.getTotalOrderCount()), daily.getOrderTrend(), "#3f8600"),
                    new SalesTradeDashboardVO.StatCard("待处理订单", String.valueOf(daily.getPendingOrderCount()), daily.getPendingTrend(), "#cf1322"),
                    new SalesTradeDashboardVO.StatCard("平均客单价", "¥" + daily.getAvgOrderValue(), daily.getAvgValueTrend(), "#3f8600")
            ));
        }

        // 2. 获取 24h 趋势
        List<SalesHourlyTrend> trends = hourlyTrendMapper.selectList(
                Wrappers.<SalesHourlyTrend>lambdaQuery()
                        .eq(SalesHourlyTrend::getStatDate, today)
                        .orderByAsc(SalesHourlyTrend::getHourNode)
        );
        vo.setChartData(trends.stream().map(t -> {
            SalesTradeDashboardVO.ChartNode node = new SalesTradeDashboardVO.ChartNode();
            node.setName(t.getHourNode());
            node.setSales(t.getSalesAmount());
            return node;
        }).collect(Collectors.toList()));

        // 3. 获取需关注的订单风险状态 (从现有的 t_system_anomaly_queue 转换)
// 优化策略：基于新增的 business_type 字段精准命中联合索引 (business_type, status, create_time)，拒绝全表扫描
        List<SystemAnomalyQueue> anomalyList = anomalyQueueMapper.selectList(
                Wrappers.<SystemAnomalyQueue>lambdaQuery()
                        .eq(SystemAnomalyQueue::getBusinessType, 1) // 1: 订单域 (固定标识或使用全局枚举/常量，如 BusinessTypeEnum.ORDER.getCode())
                        .eq(SystemAnomalyQueue::getStatus, 0)       // 0: 待处理/未解决的异常
                        .orderByDesc(SystemAnomalyQueue::getCreateTime) // 按最新的时间倒序
                        .last("LIMIT 5")                             // 限制大屏展示前 5 条核心风险
        );

// 4. 直接流式解析到 Map 容器中，直接对接前端 AntD Table 结构
        List<Map<String, Object>> riskOrders = anomalyList.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();

            // 基础键与编号
            map.put("key", a.getId().toString());
            map.put("id", a.getTargetObject() != null ? a.getTargetObject() : "SYS-" + a.getId());

            // 【核心优化点】直接从结构化字段中取值，拒绝任何拼凑或硬编码字符串
            map.put("user", a.getBuyerName() != null ? a.getBuyerName() : "系统触发");
            map.put("amount", a.getOrderAmount() != null ? "¥" + a.getOrderAmount().setScale(2).toString() : "—");

            // 状态标签与前端渲染的时间（可后续利用 Duration 算时间差，本地测试先给默认值）
            map.put("status", a.getAnomalyType());
            map.put("time", "刚刚");

            return map;
        }).collect(Collectors.toList());

        vo.setRiskOrders(riskOrders);

        return vo;
    }
}
