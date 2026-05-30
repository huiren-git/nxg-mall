package com.nxg.mall.analytics.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.analytics.entity.AdsSalesForecast;
import com.nxg.mall.analytics.vo.SalesForecastVO;

public interface IAdsSalesForecastService extends IService<AdsSalesForecast> {

    /**
     * 获取销售预测数据面板
     * @param modelName 模型名称 (prophet, arima, lstm)
     * @param category 分类 (all, digital)
     * @return 预测面板组合数据
     */
    SalesForecastVO getSalesForecastDashboard(String modelName, String category);
}
