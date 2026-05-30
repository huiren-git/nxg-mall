package com.nxg.mall.analytics.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.analytics.entity.PurchaseLog;

import java.util.List;

public interface IPurchaseLogService extends IService<PurchaseLog> {
    /**
     * 根据用户ID获取购买日志列表
     */
    List<PurchaseLog> getByUserId(Long userId);
}
