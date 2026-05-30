package com.nxg.mall.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nxg.mall.analytics.entity.PurchaseLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PurchaseLogMapper extends BaseMapper<PurchaseLog> {
    // 基础的 CRUD 已经由 MyBatis-Plus 提供
}
