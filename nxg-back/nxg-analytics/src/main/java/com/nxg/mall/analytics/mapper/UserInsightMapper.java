package com.nxg.mall.analytics.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserInsightMapper {
    Map<String, Object> getGlobalMetrics();
    List<Map<String, Object>> getGeoDistribution();
    List<Map<String, Object>> getConsumptionPreference();
    List<Map<String, Object>> getPurchasePowerDistribution();
}
