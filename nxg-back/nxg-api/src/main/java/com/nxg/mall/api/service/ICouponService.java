package com.nxg.mall.api.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.common.dto.*;
import com.nxg.mall.common.entity.Coupon;
import com.nxg.mall.common.vo.CouponAvailableVO;
import com.nxg.mall.common.vo.CouponManageVO;
import com.nxg.mall.common.vo.CouponVO;
import com.nxg.mall.common.vo.ProductListVO;

import java.util.List;

public interface ICouponService extends IService<Coupon> {

    /**
     * 分页获取领券中心的优惠券列表
     * @param userId
     * @param queryDTO
     * @return
     */
    IPage<CouponVO> getCouponCenterPage(Long userId, CouponQueryDTO queryDTO);


    List<CouponAvailableVO> findOrderAvailableCoupons(Long userId, OrderCouponCheckDTO checkDTO);

    /**
     * 分页获取优惠券适用商品列表
     * @param couponId
     * @param current
     * @param size
     * @param sortField
     * @param sortOrder
     * @return
     */
    IPage<ProductListVO> getProductsByCouponId(Long couponId, Integer current,
                                                  Integer size, String sortField, String sortOrder);

    /**
     * 创建优惠券
     * @param saveDTO
     */
    void saveCouponWithScope(CouponSaveDTO saveDTO);

    /**
     *  sales和admin后台分页获取优惠券列表
     * @param queryDTO
     * @return
     */
    Page<CouponManageVO> getCouponManagePage(CouponManageQueryDTO queryDTO);

    /**
     * 修改优惠券信息
     */
    void updateCoupon(CouponUpdateDTO updateDTO);

    /**
     * 切换优惠券状态（启用/禁用）
     */
    boolean toggleStatus(Long id, Integer status);

    /**
     * 从回收站恢复优惠券
     */
    boolean restoreCoupon(Long id);
}