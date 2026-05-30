package com.nxg.mall.analytics.vo;

import lombok.Data;
import java.util.List;

@Data
public class UserStatisticsVO {
    private SummaryData summaryData;
    private ChartData chartData;

    @Data
    public static class SummaryData {
        private Integer visitors;       // 对应 uv
        private Integer pageViews;      // 对应 pv
        private Integer newUsers;       // 对应 new_users
        private Integer tradeUsers;     // 对应 trade_users
        private Integer rechargeUsers;  // 对应 recharge_users
    }

    @Data
    public static class ChartData {
        private List<String> chartDays;        // x轴日期，例如 ["05-01", "05-02"]
        private List<Integer> visitorsSeries;  // 对应访客数(uv)趋势
        private List<Integer> newUsersSeries;  // 对应新增注册(new_users)趋势
        private List<Integer> rechargeSeries;  // 对应新增充值用户(recharge_users)趋势
    }
}
