package com.nxg.mall.analytics.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.analytics.entity.SystemAnomalyQueue;
import com.nxg.mall.analytics.vo.AnomalyDashboardVO;
import com.nxg.mall.analytics.vo.AnomalyQueueVO;

import java.util.List;

public interface ISystemAnomalyQueueService extends IService<SystemAnomalyQueue> {

    /**
     * 获取仪表盘核心统计数据
     */
    AnomalyDashboardVO getDashboardStats();

    /**
     * 获取待处理的异常队列
     */
    List<AnomalyQueueVO> getPendingQueue();

    /**
     * 处理异常 (拦截或忽略)
     */
    void handleAnomaly(Long id, Integer action, String handlerAccount);
}