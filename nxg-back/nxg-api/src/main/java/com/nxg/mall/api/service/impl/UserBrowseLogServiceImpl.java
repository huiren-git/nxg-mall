package com.nxg.mall.api.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.common.entity.UserBrowseLog;
import com.nxg.mall.api.mapper.UserBrowseLogMapper;
import com.nxg.mall.api.service.UserBrowseLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserBrowseLogServiceImpl extends ServiceImpl<UserBrowseLogMapper, UserBrowseLog> implements UserBrowseLogService {

    @Override
    @Transactional
    public Long recordEnter(Long userId, Long productId, Long categoryId, String ip) {
        UserBrowseLog log = new UserBrowseLog();
        log.setUserId(userId);
        log.setProductId(productId);
        log.setCategoryId(categoryId);
        log.setEnterTime(LocalDateTime.now());
        log.setClientIp(ip);
        // 保存并获取自增ID
        this.save(log);
        return log.getId();
    }

    @Override
    @Transactional
    public void recordLeave(Long logId, Integer duration) {
        UserBrowseLog log = this.getById(logId);
        if (log != null) {
            log.setLeaveTime(LocalDateTime.now());
            log.setDuration(duration); // 前端计算好的秒数
            this.updateById(log);
        }
    }
}
