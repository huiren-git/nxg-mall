package com.nxg.mall.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nxg.mall.analytics.entity.ProductAnalysis;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface ProductAnalysisMapper extends BaseMapper<ProductAnalysis> {

    /**
     * 获取时段内的库存总价值
     */
    BigDecimal getTotalInventoryValue(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 获取时段内的各品类销量贡献
     */
    List<Map<String, Object>> getCategorySalesVolume(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
