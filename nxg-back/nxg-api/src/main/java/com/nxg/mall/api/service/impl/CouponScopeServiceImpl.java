package com.nxg.mall.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.common.entity.CouponScope;
import com.nxg.mall.api.mapper.CouponScopeMapper;
import com.nxg.mall.api.service.ICouponScopeService;
import org.springframework.stereotype.Service;

@Service
public class CouponScopeServiceImpl extends ServiceImpl<CouponScopeMapper, CouponScope> implements ICouponScopeService {

    @Override
    public void removeByCouponId(Long couponId) {
        this.remove(new LambdaQueryWrapper<CouponScope>().eq(CouponScope::getCouponId, couponId));
    }
}