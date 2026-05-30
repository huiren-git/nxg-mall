package com.nxg.mall.analytics.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.common.entity.log.UserBrowseLog;
import com.nxg.mall.common.vo.BrowseLogSalesVO;
import com.nxg.mall.common.vo.BrowseLogVO;

public interface IUserBrowseLogService extends IService<UserBrowseLog> {

    IPage<BrowseLogSalesVO> getBrowseLogSalesPage(Page<BrowseLogSalesVO> page, Long userId);

    /**
     * 获取用户中心的足迹分页列表
     */
    IPage<BrowseLogVO> getUserBrowseLogPage(Page<BrowseLogVO> page, Long userId);
}
