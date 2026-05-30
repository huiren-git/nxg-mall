package com.nxg.mall.analytics.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.analytics.entity.PurchaseLog;
import com.nxg.mall.analytics.mapper.PurchaseLogMapper;
import com.nxg.mall.analytics.service.IPurchaseLogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PurchaseLogServiceImpl extends ServiceImpl<PurchaseLogMapper, PurchaseLog> implements IPurchaseLogService {

    @Override
    public List<PurchaseLog> getByUserId(Long userId) {
        return this.lambdaQuery()
                .eq(PurchaseLog::getUserId, userId)
                .orderByDesc(PurchaseLog::getPurchaseTime) // 按时间倒序
                .list();
    }
}
