package com.nxg.mall.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nxg.mall.common.vo.UserCenterVO;
import com.nxg.mall.common.vo.UserOrderItemVO;
import com.nxg.mall.common.entity.Cart;
import com.nxg.mall.common.entity.Order;
import com.nxg.mall.common.entity.OrderItem;
import com.nxg.mall.common.entity.User;
import com.nxg.mall.api.mapper.CartMapper;
import com.nxg.mall.api.mapper.OrderItemMapper;
import com.nxg.mall.api.mapper.OrderMapper;
import com.nxg.mall.api.mapper.UserMapper;
import com.nxg.mall.api.service.UserCenterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserCenterServiceImpl implements UserCenterService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;

    @Override
    public UserCenterVO getUserCenterInfo(Long userId) {
        UserCenterVO vo = new UserCenterVO();

        // 1. 获取用户信息 (头像、昵称、余额)
        User user = userMapper.selectById(userId);
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setBalance(user.getBalance());
            vo.setEmail(user.getEmail());
        }

        // 2. 获取购物车商品数量
        Long cartCount = cartMapper.selectCount(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId));
        vo.setCartCount(cartCount.intValue());

        // 3. 获取优惠券数量 (目前模拟为0，待后续实现)
        vo.setCouponCount(0);

        // 4. TODO: 浏览历史 (暂时占位)
        vo.setBrowsingHistory("TODO: 浏览历史逻辑开发中");

        // 5. 获取订单相关信息 (聚合 Order 和 OrderItem)
        // 逻辑：先查出该用户最近的5笔订单，再查出这些订单对应的明细
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime)
                .last("limit 5"));

        List<UserOrderItemVO> itemVOs = new ArrayList<>();
        for (Order order : orders) {
            // 查询该订单下的所有明细
            List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderNo, order.getOrderNo()));

            for (OrderItem item : items) {
                UserOrderItemVO itemVO = new UserOrderItemVO();
                itemVO.setOrderNo(order.getOrderNo());
                itemVO.setStatus(order.getStatus());
                itemVO.setCreateTime(order.getCreateTime());

                // 明细信息
                itemVO.setProductName(item.getProductName());
                itemVO.setProductPic(item.getProductPic());
                itemVO.setQuantity(item.getQuantity());
                itemVO.setPayAmount(item.getPayAmount());

                itemVOs.add(itemVO);
            }
        }
        vo.setRecentOrders(itemVOs);

        return vo;
    }
}
