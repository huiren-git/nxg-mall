package com.nxg.mall.api.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nxg.mall.api.vo.PurchaseLogVO;
import com.nxg.mall.common.entity.Order;
import com.nxg.mall.common.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    /**
     * 根据订单号查询订单
     * 默认实现：利用 MyBatis-Plus 的 LambdaQueryWrapper
     */
    default Order selectByOrderNo(String orderNo) {
        return selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
    }

    // 自定义联表查询
    List<OrderVO> selectUserOrderList(@Param("userId") Long userId, @Param("statusList") List<Integer> statusList);

    // 获取用户的购物日志
    List<PurchaseLogVO> selectPurchaseLogs(Long userId);
}
