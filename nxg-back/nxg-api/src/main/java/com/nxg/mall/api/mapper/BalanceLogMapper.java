package com.nxg.mall.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nxg.mall.common.entity.UserBalanceLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BalanceLogMapper extends BaseMapper<UserBalanceLog> {
}
