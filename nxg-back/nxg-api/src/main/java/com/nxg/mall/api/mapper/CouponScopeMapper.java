package com.nxg.mall.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nxg.mall.common.entity.CouponScope;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Mapper
public interface CouponScopeMapper extends BaseMapper<CouponScope> {
}
