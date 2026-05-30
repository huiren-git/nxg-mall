package com.nxg.mall.api.controller;

import com.nxg.mall.common.Result;
import com.nxg.mall.api.service.CouponUserService;
import com.nxg.mall.common.utils.SecurityUtils;
import com.nxg.mall.common.vo.CouponUserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coupons")
public class CouponUserController {

    @Autowired
    private CouponUserService couponUserService;

    /**
     * 用户领取优惠券
     * @param id 优惠券ID
     * @return 结果
     */
    @PostMapping("/collect/{id}")
    public Result<Void> collect(@PathVariable Long id) {
        // 从当前登录上下文获取用户ID (假设你已实现 UserContext)
        Long userId = SecurityUtils.getUserId();
        couponUserService.collectCoupon(id, userId);
        return Result.success();
    }

    /**
     * 获取当前用户的优惠券列表
     * @param status 状态筛选: 1-未使用, 2-已使用, 3-已过期 (不传则查询全部)
     */
    @GetMapping("/my")
    public Result<List<CouponUserVO>> getMyCoupons(@RequestParam(required = false) Integer status) {
        // 从安全上下文获取当前登录用户ID
        Long userId = SecurityUtils.getUserId();
        List<CouponUserVO> list = couponUserService.getUserCouponList(userId, status);
        return Result.success(list);
    }
}
