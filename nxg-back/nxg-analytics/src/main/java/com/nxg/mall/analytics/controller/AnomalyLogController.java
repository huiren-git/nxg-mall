package com.nxg.mall.analytics.controller;

import com.nxg.mall.analytics.service.IAnomalyLogService;
import com.nxg.mall.analytics.vo.AnomalyDashboardVO;
import com.nxg.mall.analytics.vo.AnomalyLogVO;
import com.nxg.mall.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/anomalies")
public class AnomalyLogController {

    @Autowired
    private IAnomalyLogService anomalyLogService;

    /**
     * GET /api/anomalies/metrics
     * 获取仪表盘实时统计数据与图表
     */
    @GetMapping("/metrics")
    public Result<AnomalyDashboardVO> getDashboardMetrics() {
        return Result.success(anomalyLogService.getDashboardMetrics());
    }

    /**
     * GET /api/anomalies/pending
     * 获取待处理异常列表
     */
    @GetMapping("/pending")
    public Result<List<AnomalyLogVO>> getPendingLogs() {
        return Result.success(anomalyLogService.getPendingLogs());
    }

    /**
     * PUT /api/anomalies/{logNo}/intercept
     * 针对单条异常执行“拦截”操作
     */
    @PutMapping("/{logNo}/intercept")
    public Result<Void> interceptLog(@PathVariable String logNo) {
        boolean success = anomalyLogService.handleLog(logNo, 1);
        return success ? Result.success() : Result.error("操作失败");
    }

    /**
     * PUT /api/anomalies/{logNo}/ignore
     * 针对单条异常执行“忽略”操作
     */
    @PutMapping("/{logNo}/ignore")
    public Result<Void> ignoreLog(@PathVariable String logNo) {
        boolean success = anomalyLogService.handleLog(logNo, 2);
        return success ? Result.success() : Result.error("操作失败");
    }

    /**
     * PUT /api/anomalies/handle-all
     * 将全部待处理记录标记为已读(忽略)
     */
    @PutMapping("/handle-all")
    public Result<Void> handleAllPendingLogs() {
        anomalyLogService.handleAllPending();
        return Result.success();
    }
}