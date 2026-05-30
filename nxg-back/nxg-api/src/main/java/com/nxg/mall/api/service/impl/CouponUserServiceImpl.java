package com.nxg.mall.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nxg.mall.common.entity.Coupon;
import com.nxg.mall.common.entity.CouponUser;
import com.nxg.mall.common.exception.BusinessException;
import com.nxg.mall.api.mapper.CouponMapper;
import com.nxg.mall.api.mapper.CouponUserMapper;
import com.nxg.mall.api.service.CouponUserService;
import com.nxg.mall.common.vo.CouponUserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class CouponUserServiceImpl implements CouponUserService {

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private CouponUserMapper couponUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectCoupon(Long couponId, Long userId) {
        // 1. 查询优惠券基本信息
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getIsDeleted() == 1) {
            throw new BusinessException("优惠券不存在");
        }

        // 2. 校验状态与时间
        if (coupon.getStatus() != 1) {
            throw new BusinessException("该优惠券暂未上架");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
            throw new BusinessException("不在优惠券领用时间内");
        }

        // 3. 校验库存（这里先做一次软校验）
        if (coupon.getTakeCount() >= coupon.getTotalCount()) {
            throw new BusinessException("优惠券已被抢光了");
        }

        // 4. 校验是否重复领取
        // 将 Integer 改为 Long
        Long count = couponUserMapper.selectCount(new LambdaQueryWrapper<CouponUser>()
                .eq(CouponUser::getUserId, userId)
                .eq(CouponUser::getCouponId, couponId));

// 注意：Long 类型的比较建议使用 > 0L
        if (count != null && count > 0) {
            throw new BusinessException("您已经领过该优惠券了");
        }

        // 5. 核心：原子扣减库存 (使用 SQL 行锁防止超卖)
        int updateRows = couponMapper.increaseTakeCount(couponId);
        if (updateRows == 0) {
            throw new BusinessException("优惠券已被抢光了");
        }

        // 6. 保存领取记录
        CouponUser couponUser = new CouponUser();
        couponUser.setUserId(userId);
        couponUser.setCouponId(couponId);
        couponUser.setStatus(1); // 1: 未使用
        couponUserMapper.insert(couponUser);
    }

    @Override
    public List<CouponUserVO> getUserCouponList(Long userId, Integer status) {
        // 调用 Mapper 进行关联查询
        return couponUserMapper.selectUserCouponList(userId, status);
    }
}
