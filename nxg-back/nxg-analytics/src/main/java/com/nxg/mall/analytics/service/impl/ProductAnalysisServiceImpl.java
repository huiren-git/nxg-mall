package com.nxg.mall.analytics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.analytics.entity.ProductAnalysis;
import com.nxg.mall.analytics.entity.ProductMetricDetail;
import com.nxg.mall.analytics.entity.ProductSalesInsight;
import com.nxg.mall.analytics.entity.ProductSalesTrend;
import com.nxg.mall.analytics.mapper.ProductAnalysisMapper;
import com.nxg.mall.analytics.mapper.ProductMetricDetailMapper;
import com.nxg.mall.analytics.mapper.ProductSalesInsightMapper;
import com.nxg.mall.analytics.mapper.ProductSalesTrendMapper;
import com.nxg.mall.analytics.service.IProductAnalysisService;
import com.nxg.mall.analytics.vo.ProductAnalysisVO;
import com.nxg.mall.analytics.vo.ProductDashboardVO;
import com.nxg.mall.analytics.vo.ProductSalesPivotVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductAnalysisServiceImpl extends ServiceImpl<ProductAnalysisMapper, ProductAnalysis> implements IProductAnalysisService {

    @Autowired
    private ProductSalesInsightMapper insightMapper;
    @Autowired
    private ProductSalesTrendMapper trendMapper;
    @Autowired
    private ProductMetricDetailMapper detailMapper;

    @Override
    public ProductDashboardVO getDashboardData(LocalDate startDate, LocalDate endDate) {
        ProductDashboardVO vo = new ProductDashboardVO();

        // 💡 提示：对于大盘的 Mapper 自定义 SQL 查询（如求总价值、销售占比），你可能也需要修改对应的 XML/注解，把这两个时间传进去作为过滤条件
        // 这里先为你展示在 MyBatis-Plus 自带 Wrapper 中的用法

        // 1. 计算指标 (加入时间范围过滤)
        LambdaQueryWrapper<ProductAnalysis> alertWrapper = new LambdaQueryWrapper<ProductAnalysis>()
                .lt(ProductAnalysis::getStock, 10)
                .ge(startDate != null, ProductAnalysis::getDt, startDate) // 💡 大于等于开始时间
                .le(endDate != null, ProductAnalysis::getDt, endDate);    // 💡 小于等于结束时间

        long count = this.count(alertWrapper);
        vo.setAlertSkuCount(Math.toIntExact(count));

        // 2. 传参给 Mapper 查询销售贡献与库存总价值
        // 建议修改 baseMapper 的方法接收这两个时间，在 SQL 里通过 BETWEEN #{startDate} AND #{endDate} 过滤
        vo.setTotalInventoryValue(baseMapper.getTotalInventoryValue(startDate, endDate));
        vo.setCategorySales(baseMapper.getCategorySalesVolume(startDate, endDate));

        // 3. 模拟平均周转率
        vo.setAvgTurnoverRate(new BigDecimal("72.4"));

        return vo;
    }

    @Override
    public IPage<ProductAnalysisVO> getProductPage(int current, int size, LocalDate startDate, LocalDate endDate, String category, String status) {
        // 1. 初始化原始实体的 Page 对象
        Page<ProductAnalysis> entityPage = new Page<>(current, size);
        LambdaQueryWrapper<ProductAnalysis> wrapper = new LambdaQueryWrapper<>();

        // 2. 拼接条件查询 (动态加入时间范围判定)
        wrapper.eq(StringUtils.hasText(category), ProductAnalysis::getCategory, category)
                .eq(StringUtils.hasText(status), ProductAnalysis::getStatus, status)
                .ge(startDate != null, ProductAnalysis::getDt, startDate) // 💡 动态拼装：起始时间
                .le(endDate != null, ProductAnalysis::getDt, endDate)     // 💡 动态拼装：结束时间
                .orderByDesc(ProductAnalysis::getSalesVolume);

        // 3. 执行物理分页查询
        this.page(entityPage, wrapper);

        // 4. 利用 page.convert 映射到 VO 结构并返回
        return entityPage.convert(entity -> {
            ProductAnalysisVO vo = new ProductAnalysisVO();
            vo.setId(entity.getId());
            vo.setName(entity.getProductName());
            vo.setCategory(entity.getCategory());
            vo.setStock(entity.getStock());
            vo.setStatus(entity.getStatus());
            vo.setSalesVolume(entity.getSalesVolume());
            vo.setTurnover(entity.getTurnoverRate());
            return vo;
        });
    }

    @Override
    public ProductSalesPivotVO getProductPivotDetails(Long productId) {
        // 1. 获取基础与安全画像
        ProductSalesInsight insight = insightMapper.selectById(productId);
        if (insight == null) {
            return null;
        }

        // 2. 获取趋势数据并按 sort_order 排序
        List<ProductSalesTrend> rawTrends = trendMapper.selectList(
                Wrappers.<ProductSalesTrend>lambdaQuery()
                        .eq(ProductSalesTrend::getProductId, productId)
                        .orderByAsc(ProductSalesTrend::getSortOrder)
        );

        // 3. 获取底部指标明细
        List<ProductMetricDetail> details = detailMapper.selectList(
                Wrappers.<ProductMetricDetail>lambdaQuery().eq(ProductMetricDetail::getProductId, productId)
        );

        // 4. 装配聚合大对象 VO
        ProductSalesPivotVO vo = new ProductSalesPivotVO();
        vo.setProductId(insight.getProductId());
        vo.setProductName(insight.getProductName());
        vo.setCurrentStock(insight.getCurrentStock());
        vo.setAvgDailySales(insight.getAvgDailySales());
        vo.setDaysToOutOfStock(insight.getDaysToOutOfStock());
        vo.setRestockDeadlineDays(insight.getRestockDeadlineDays());

        // 简单安全策略：根据剩余天数动态算一个周转进度条百分比
        // 例如：大于15天算安全(100%)，小于则等比例缩放
        int percent = Math.min(100, (insight.getDaysToOutOfStock() * 100) / 15);
        vo.setStockUrgencyPercent(percent);

        // 组装评估对象
        ProductSalesPivotVO.EvaluationInfo evalVo = new ProductSalesPivotVO.EvaluationInfo();
        evalVo.setScore(insight.getEvaluationScore());
        evalVo.setHealthStatus(insight.getHealthStatus());
        evalVo.setRiskWarning(insight.getRiskWarning());
        evalVo.setSuggestedAction(insight.getSuggestedAction());
        vo.setEvaluation(evalVo);

        // 组装趋势图表
        List<ProductSalesPivotVO.TrendItem> trendVos = rawTrends.stream().map(t -> {
            ProductSalesPivotVO.TrendItem item = new ProductSalesPivotVO.TrendItem();
            item.setPeriod(t.getPeriodName());
            item.setSales(t.getSalesVolume());
            item.setIsForecast(t.getIsForecast() == 1);
            return item;
        }).collect(Collectors.toList());
        vo.setTrends(trendVos);

        // 组装明细表格
        vo.setMetricsTable(details);

        return vo;
    }
}
