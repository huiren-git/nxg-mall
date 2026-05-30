package com.nxg.mall.analytics.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductRankOverviewVO {
    // 前10个上榜商品的平均客单价
    private BigDecimal averagePrice;

    // 排行榜列表数据
    private List<ProductRankVO> leaderboard;
}
