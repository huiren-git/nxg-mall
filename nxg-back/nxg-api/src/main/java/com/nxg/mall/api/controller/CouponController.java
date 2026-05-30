package com.nxg.mall.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nxg.mall.common.Result; // 假设你有一个通用的返回体
import com.nxg.mall.common.dto.*;
import com.nxg.mall.api.service.ICouponService;
import com.nxg.mall.common.utils.SecurityUtils;
import com.nxg.mall.common.vo.CouponAvailableVO;
import com.nxg.mall.common.vo.CouponManageVO;
import com.nxg.mall.common.vo.CouponVO;
import com.nxg.mall.common.vo.ProductListVO;
import com.nxg.mall.common.annotation.Log;
import com.nxg.mall.common.enums.ActionType;
import com.nxg.mall.common.enums.TargetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
public class CouponController {

    @Autowired
    private ICouponService couponService;

    /**
     * 分页查询领券中心列表
     */
    @GetMapping("/coupons/page")
    public Result<IPage<CouponVO>> getCouponCenterPage(CouponQueryDTO queryDTO) {
        // 1. 获取用户id
        Long currentUserId = SecurityUtils.getUserId();
        return Result.success(couponService.getCouponCenterPage(currentUserId, queryDTO));
    }

    /**
     * 获取优惠券适用商品列表（支持分页排序）
     */
    /**
     * C端：获取某张优惠券适用的商品列表 (点击去使用)
     */
    @GetMapping("/coupons/{couponId}/products")
    public Result<IPage<ProductListVO>> getCouponAvailableProducts(
            @PathVariable Long couponId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "default") String sortField,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        // 调用专门为 C 端编写的 service 方法
        IPage<ProductListVO> page = couponService.getProductsByCouponId(
                couponId, current, size, sortField, sortOrder);
        return Result.success(page);
    }

    @PostMapping("/coupons/order-available")
    public Result<List<CouponAvailableVO>> getOrderAvailableCoupons(@RequestBody OrderCouponCheckDTO checkDTO) {
        // 假设从 SecurityContext 获取当前登录用户ID
        Long userId = SecurityUtils.getUserId();
        List<CouponAvailableVO> list = couponService.findOrderAvailableCoupons(userId, checkDTO);
        return Result.success(list);
    }

    // 1. 分页查询优惠券列表 (支持按状态过滤)
    @GetMapping("/sales/coupons/page")
    public Result<Page<CouponManageVO>> getCouponManagePage(CouponManageQueryDTO queryDTO) {
        Page<CouponManageVO> result = couponService.getCouponManagePage(queryDTO);
        return Result.success(result);
    }

    // 2. 创建优惠券
    @Log(title = "创建优惠券", actionType = ActionType.ADD_COUPON, targetType = TargetType.COUPON)
    @PostMapping("/sales/coupons")
    public Result<Void> createCoupon(@RequestBody CouponSaveDTO saveDTO) {
        couponService.saveCouponWithScope(saveDTO);
        return Result.success();
    }

    // 3. 修改优惠券
    @Log(title = "修改优惠券", actionType = ActionType.UPDATE_COUPON, targetType = TargetType.COUPON)
    @PutMapping("/sales/coupons/{id}")
    public Result<Void> updateCoupon(@PathVariable Long id, @Validated @RequestBody CouponUpdateDTO updateDTO) {
        // 强制设置ID，确保路径ID与对象ID一致
        updateDTO.setId(id);
        couponService.updateCoupon(updateDTO);
        return Result.success();
    }

    // 4. 启用/禁用状态切换
    @Log(title = "修改优惠券", actionType = ActionType.UPDATE_COUPON, targetType = TargetType.COUPON)
    @PutMapping("/sales/coupons/{id}/status/{status}")
    public Result toggleStatus(@PathVariable Long id, @PathVariable Integer status) {
        return couponService.toggleStatus(id, status) ? Result.success("状态已更新") : Result.error("操作失败");
    }

    // 5. 逻辑删除 (移入回收站)
    @Log(title = "删除优惠券", actionType = ActionType.DELETE_COUPON, targetType = TargetType.COUPON)
    @DeleteMapping("/sales/coupons/{id}")
    public Result removeCoupon(@PathVariable Long id) {
        return couponService.removeById(id) ? Result.success("已移入回收站") : Result.error("删除失败");
    }

    // 6. 恢复优惠券
    @Log(title = "恢复优惠券", actionType = ActionType.UPDATE_COUPON, targetType = TargetType.COUPON)
    @PutMapping("/sales/coupons/{id}/restore")
    public Result restoreCoupon(@PathVariable Long id) {
        return couponService.restoreCoupon(id) ? Result.success("已恢复") : Result.error("恢复失败");
    }
}
