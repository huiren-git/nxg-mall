package com.nxg.mall.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nxg.mall.analytics.entity.ProductSalesRank;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductSalesRankMapper extends BaseMapper<ProductSalesRank> {
    // MyBatis-Plus 已内置常用 CRUD，排名逻辑可在 Service 中使用 Wrapper 构建
}
