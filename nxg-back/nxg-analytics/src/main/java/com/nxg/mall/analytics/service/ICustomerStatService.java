package com.nxg.mall.analytics.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.analytics.dto.UserPersonaDTO;
import com.nxg.mall.analytics.entity.CustomerStat;
import com.nxg.mall.analytics.vo.TrendDataVO;
import com.nxg.mall.analytics.vo.UserPersonaVO;
import com.nxg.mall.analytics.vo.UserSummaryVO;
import com.nxg.mall.common.entity.log.LogMessage;
import com.nxg.mall.analytics.model.dto.CustomerQueryDTO;

import java.time.LocalDate;

public interface ICustomerStatService extends IService<CustomerStat> {
    IPage<CustomerStat> getCustomerPage(CustomerQueryDTO queryDTO);

    // 处理 Kafka 传来的实时日志（地域、标签）
    void processUserLog(LogMessage logMessage);

    // 定期计算购买力（批处理）
    void calculatePurchasingPower();

    /**
     * 获取用户画像详情
     */
    UserPersonaVO getPersonaByUserId(Long userId);

    UserSummaryVO getSummary(LocalDate start, LocalDate end);

    TrendDataVO getTrend(String type, LocalDate start, LocalDate end);
}
