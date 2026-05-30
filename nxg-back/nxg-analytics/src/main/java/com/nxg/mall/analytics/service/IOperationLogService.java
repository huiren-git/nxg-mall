package com.nxg.mall.analytics.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.analytics.dto.OperationLogQueryDTO;
import com.nxg.mall.analytics.entity.OperationLog;

public interface IOperationLogService extends IService<OperationLog> {
    /**
     * 分页查询审计日志
     */
    IPage<OperationLog> getLogPage(OperationLogQueryDTO queryDTO);
}
