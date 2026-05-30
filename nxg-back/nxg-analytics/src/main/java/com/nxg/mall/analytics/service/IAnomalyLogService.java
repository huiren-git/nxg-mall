package com.nxg.mall.analytics.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.analytics.entity.AnomalyLog;
import com.nxg.mall.analytics.vo.AnomalyDashboardVO;
import com.nxg.mall.analytics.vo.AnomalyLogVO;

import java.util.List;

public interface IAnomalyLogService extends IService<AnomalyLog> {

    /**
     * 获取异常监控仪表盘实时数据 (Mock Kafka/Redis)
     */
    AnomalyDashboardVO getDashboardMetrics();

    /**
     * 获取待处理的异常列表 (读取 MySQL)
     */
    List<AnomalyLogVO> getPendingLogs();

    /**
     * 处理单个异常状态 (1-拦截, 2-忽略)
     */
    boolean handleLog(String logNo, Integer actionType);

    /**
     * 全部标记为已读/忽略
     */
    boolean handleAllPending();
}