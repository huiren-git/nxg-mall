package com.nxg.mall.analytics.service.impl;

import com.nxg.mall.analytics.mapper.UserInsightMapper;
import com.nxg.mall.analytics.vo.UserInsightVO;
import com.nxg.mall.analytics.service.IUserInsightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserInsightServiceImpl implements IUserInsightService {

    @Autowired
    private UserInsightMapper userInsightMapper;

    @Override
    public UserInsightVO getUserInsightData(LocalDate date) {
        UserInsightVO vo = new UserInsightVO();

        // 1. 基础指标计算映射
        Map<String, Object> metrics = userInsightMapper.getGlobalMetrics();
        if (metrics != null) {
            // 🌟 安全写法：利用 Object 的 toString() 兜底，再解析
            vo.setTotalActiveUsers(Integer.valueOf(metrics.getOrDefault("totalActiveUsers", "0").toString()));

            // SUM 算出来经常是 BigDecimal，这样写绝对不会报错
            vo.setCorePaidUsers(Integer.valueOf(metrics.getOrDefault("corePaidUsers", "0").toString()));

            vo.setAvgCustomerValue(new BigDecimal(metrics.getOrDefault("avgCustomerValue", "0.00").toString()));
            vo.setRepurchaseRate(new BigDecimal(metrics.getOrDefault("repurchaseRate", "0.00").toString()));
        }

        // 2. 地理 Top5 分布映射
        vo.setGeoDistribution(userInsightMapper.getGeoDistribution());

        // 3. 购买力划分处理
        List<Map<String, Object>> powerList = userInsightMapper.getPurchasePowerDistribution();
        List<UserInsightVO.PowerRatio> powerRatios = powerList.stream().map(m ->
                new UserInsightVO.PowerRatio(
                        m.get("name").toString(),
                        new BigDecimal(m.get("value").toString())
                )
        ).collect(Collectors.toList());
        vo.setPurchasePower(powerRatios);

        // 4. 雷达图消费偏好转换处理
        List<Map<String, Object>> prefList = userInsightMapper.getConsumptionPreference();
        Map<String, Integer> preferenceMap = new HashMap<>();
        for (Map<String, Object> m : prefList) {
            if (m.get("value") != null) {
                // 🌟 安全写法：转成 String 再转为 Integer
                int val = Integer.parseInt(m.get("value").toString());
                preferenceMap.put(m.get("name").toString(), val);
            }
        }
        vo.setConsumptionPreference(preferenceMap);

        return vo;
    }
}