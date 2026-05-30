package com.nxg.mall.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.common.entity.UserStatus;

public interface IUserStatusService extends IService<UserStatus> {
    // 自定义业务方法：初始化用户状态
    void initUserStatus(Long userId);
}