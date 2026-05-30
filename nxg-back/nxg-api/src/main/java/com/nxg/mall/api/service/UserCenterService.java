package com.nxg.mall.api.service;

import com.nxg.mall.common.vo.UserCenterVO;

/**
 * 用户中心业务接口
 * 负责聚合用户基本信息、统计数据及最近订单快照
 */
public interface UserCenterService {

    /**
     * 获取用户中心门户页面的全量聚合数据
     * * @param userId 当前登录用户ID
     * @return 包含用户信息、购物车数、优惠券数及最近订单的VO对象
     */
    UserCenterVO getUserCenterInfo(Long userId);
}