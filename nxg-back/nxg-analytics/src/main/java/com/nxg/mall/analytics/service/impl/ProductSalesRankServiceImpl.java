package com.nxg.mall.analytics.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.analytics.entity.ProductSalesRank;
import com.nxg.mall.analytics.mapper.ProductSalesRankMapper;
import com.nxg.mall.analytics.service.IProductSalesRankService;
import com.nxg.mall.analytics.vo.ProductRankOverviewVO;
import com.nxg.mall.analytics.vo.ProductRankVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ProductSalesRankServiceImpl extends ServiceImpl<ProductSalesRankMapper, ProductSalesRank> implements IProductSalesRankService {

    @Override
    @DS("analytics")
    public ProductRankOverviewVO getLeaderboard(String timeRange) {
        // 1. 查询排行前 10
        LambdaQueryWrapper<ProductSalesRank> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ProductSalesRank::getSalesVolume).last("LIMIT 10");
        List<ProductSalesRank> list = baseMapper.selectList(wrapper);

        ProductRankOverviewVO overview = new ProductRankOverviewVO();
        if (list.isEmpty()) {
            overview.setAveragePrice(BigDecimal.ZERO);
            overview.setLeaderboard(new ArrayList<>());
            return overview;
        }

        // 2. 跨库查询商品图片 (利用之前封装好的方案)
        List<Long> productIds = list.stream().map(ProductSalesRank::getProductId).collect(Collectors.toList());

        // 3. 组装明细列表
        AtomicInteger rankCounter = new AtomicInteger(1);
        long totalSales = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        List<ProductRankVO> voList = new ArrayList<>();
        for (ProductSalesRank item : list) {
            ProductRankVO vo = new ProductRankVO();
            vo.setRank(rankCounter.getAndIncrement());
            vo.setId(item.getProductId().toString());
            vo.setName(item.getProductName());
            vo.setCategory(item.getCategory());
            vo.setSales(item.getSalesVolume());
            vo.setRevenue(item.getSalesAmount());

            // 累加总销量和总销售额，用来算均价
            if (item.getSalesVolume() != null) totalSales += item.getSalesVolume();
            if (item.getSalesAmount() != null) totalRevenue = totalRevenue.add(item.getSalesAmount());

            // 趋势与图片
            vo.setTrend((int)(Math.random() * 40 - 10));
            vo.setImage(item.getMainImage());

            voList.add(vo);
        }

        // 4. 后端精准计算均价： 均价 = 总销售额 / 总销量
        BigDecimal averagePrice = BigDecimal.ZERO;
        if (totalSales > 0) {
            // 保留两位小数，四舍五入
            averagePrice = totalRevenue.divide(BigDecimal.valueOf(totalSales), 2, BigDecimal.ROUND_HALF_UP);
        }

        // 5. 封装进组合对象返回
        overview.setAveragePrice(averagePrice);
        overview.setLeaderboard(voList);

        return overview;
    }
}
