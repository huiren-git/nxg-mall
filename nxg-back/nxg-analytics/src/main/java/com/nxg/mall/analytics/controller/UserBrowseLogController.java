package com.nxg.mall.analytics.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nxg.mall.analytics.service.IUserBrowseLogService;
import com.nxg.mall.common.Result;
import com.nxg.mall.common.utils.SecurityUtils;
import com.nxg.mall.common.vo.BrowseLogSalesVO;
import com.nxg.mall.common.vo.BrowseLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class UserBrowseLogController {

    private final IUserBrowseLogService browseLogService;

    /**
     * 获取浏览日志列表 (分页)
     * 最终路径: GET /api/logs/browse
     */
    @GetMapping("/browse")
    public Result<IPage<BrowseLogSalesVO>> getBrowseLogs(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {

        IPage<BrowseLogSalesVO> page = browseLogService.getBrowseLogSalesPage(new Page<>(current, size), userId);
        return Result.success(page);
    }

    /**
     * C端：获取我的足迹 (用户中心)
     */
    @GetMapping("/browse/my")
    public Result<IPage<BrowseLogVO>> getMyBrowseLogs(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {

        Long userId = SecurityUtils.getUserId();

        IPage<BrowseLogVO> page = browseLogService.getUserBrowseLogPage(new Page<>(current, size), userId);
        return Result.success(page);
    }

}
