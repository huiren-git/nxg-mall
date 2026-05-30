package com.nxg.mall.analytics.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nxg.mall.common.Result; // 假设你有统一样式的返回体
import com.nxg.mall.analytics.dto.OperationLogQueryDTO;
import com.nxg.mall.analytics.entity.OperationLog;
import com.nxg.mall.analytics.service.IOperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/audit")
public class OperationLogController {

    @Autowired
    private IOperationLogService logService;

    /**
     * 获取审计日志分页列表
     * GET /api/admin/audit/logs
     */
    @GetMapping("/logs")
    public Result<IPage<OperationLog>> listLogs(OperationLogQueryDTO queryDTO) {
        // 前端传来的 timeRange [start, end] 需要在 Controller 简单处理映射到 DTO
        IPage<OperationLog> data = logService.getLogPage(queryDTO);
        return Result.success(data);
    }
}
