package com.nxg.mall.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nxg.mall.common.dto.CouponManageQueryDTO;
import com.nxg.mall.common.dto.CouponQueryDTO;
import com.nxg.mall.common.entity.Coupon;
import com.nxg.mall.api.service.bo.UserCouponDetailBO;
import com.nxg.mall.common.vo.CouponManageVO;
import com.nxg.mall.common.vo.CouponVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 原子增加领取数量，SQL 层面判断 take_count < total_count
     */
    @Update("UPDATE t_coupon SET take_count = take_count + 1 " +
            "WHERE id = #{id} AND take_count < total_count AND status = 1")
    int increaseTakeCount(@Param("id") Long id);

    /**
     * 获取领券中心的优惠券列表(关联用户的优惠券状态)
     * @param page
     * @param userId
     * @param category
     * @return
     */
     IPage<CouponVO> selectCouponCenterPage(
     IPage<Coupon> page,
     @Param("userId") Long userId,
     @Param("category") Integer category
     );

    /**
     * 获取订单可用优惠券列表
     * @param userId
     * @return
     */
    List<UserCouponDetailBO> selectUserAvailableCoupons(@Param("userId") Long userId);

    /**
     * 分页查询回收站中的优惠券（完全自定义 SQL，绕过逻辑删除插件）
     *
     * @param page     MyBatis-Plus 分页对象（必须放在第一个参数）
     * @param queryDTO 查询参数 DTO
     * @return 分页数据
     */
    Page<Coupon> selectDeletedPage(Page<Coupon> page, @Param("query") CouponManageQueryDTO queryDTO);

    // 恢复逻辑删除的优惠券
    @Update("UPDATE t_coupon SET is_deleted = 0, status = 0 WHERE id = #{id}")
    int restoreCouponById(@Param("id") Long id);
}