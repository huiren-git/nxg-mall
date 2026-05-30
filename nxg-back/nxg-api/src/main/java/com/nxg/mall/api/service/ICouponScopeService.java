package com.nxg.mall.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.common.entity.CouponScope;

public interface ICouponScopeService extends IService<CouponScope> {
    // 这里可以定义一些特定的业务方法，比如按优惠券ID清除范围
    void removeByCouponId(Long couponId);
}