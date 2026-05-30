package com.nxg.mall.analytics.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.analytics.mapper.UserBrowseLogMapper;
import com.nxg.mall.analytics.service.IUserBrowseLogService;
import com.nxg.mall.common.entity.log.UserBrowseLog;
import com.nxg.mall.common.vo.BrowseLogSalesVO;
import com.nxg.mall.common.vo.BrowseLogVO;
import org.springframework.stereotype.Service;

@Service
public class UserBrowseLogServiceImpl extends ServiceImpl<UserBrowseLogMapper, UserBrowseLog> implements IUserBrowseLogService {

    @Override
    public IPage<BrowseLogSalesVO> getBrowseLogSalesPage(Page<BrowseLogSalesVO> page, Long userId) {
        // 入参是具体的实现类 Page，出参是接口 IPage
        return baseMapper.selectBrowseLogPage(page, userId);
    }

    @Override
    public IPage<BrowseLogVO> getUserBrowseLogPage(Page<BrowseLogVO> page, Long userId) {
        return baseMapper.selectUserBrowseLogPage(page, userId);
    }
}
