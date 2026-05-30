package com.nxg.mall.api.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.common.entity.UserStatus;
import com.nxg.mall.api.mapper.UserStatusMapper;
import com.nxg.mall.api.service.IUserStatusService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserStatusServiceImpl extends ServiceImpl<UserStatusMapper, UserStatus>
        implements IUserStatusService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initUserStatus(Long userId) {
        UserStatus userStatus = new UserStatus();
        // 1. 关联用户id
        userStatus.setUserId(userId);
        // 2. 设置状态 默认正常状态
        userStatus.setStatus(1);
        // 使用父类提供的 save 方法
        this.save(userStatus);
    }
}