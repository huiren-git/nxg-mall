package com.nxg.mall.api.service;

import com.nxg.mall.common.vo.CouponUserVO;

import java.util.List;

public interface CouponUserService {
    void collectCoupon(Long couponId, Long userId);

    List<CouponUserVO> getUserCouponList(Long userId, Integer status);
}