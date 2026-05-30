package com.nxg.mall.analytics.service;

import com.nxg.mall.analytics.vo.UserInsightVO;
import java.time.LocalDate;

public interface IUserInsightService {
    /**
     * 获取指定日期的用户画像深度分析数据
     */
    UserInsightVO getUserInsightData(LocalDate date);
}