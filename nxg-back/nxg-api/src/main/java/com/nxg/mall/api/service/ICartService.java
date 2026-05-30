package com.nxg.mall.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.common.dto.CartUpdateDTO;
import com.nxg.mall.common.vo.CartVO;
import com.nxg.mall.common.entity.Cart;

import java.util.List;
import java.util.Map;

public interface ICartService extends IService<Cart> {
    /*
    * 将商品加入购物车
    * */
    void addToCart(Long userId, Long productId, Integer count);

    /*
     * 获取并展示当前用户的购物车清单
     * */
    List<CartVO> listMyCart(Long userId);

    /**
     * 获取购物车中的条目数
     * @param userId
     * @return
     */
    Integer getCartCountByUserId(Long userId);

    /**
     * 更新购物车条目
     * @param userId
     * @param updateDto
     */
    void updateCartItem(Long userId, CartUpdateDTO updateDto);

    /*
     * 批量删除购物车中的商品
     * */
    void deleteItems(Long userId, List<Long> skuIds);

    /**
     * 更新指定用户购物车内所有商品的选中状态
     * @param userId 用户ID
     * @param checked 选中状态 0-未选, 1-已选
     */
    void updateAllCheckedStatus(Long userId, Integer checked);

    // ================== 基于 Redis 的购物车操作 ==================
    /**
     * 加入购物车（已存在则累加，不存在则新增）
     */
    void addCartItem(Long userId, Long skuId, Integer quantity);

    /**
     * 覆盖修改指定 skuId 的数量
     */
    void updateCartItem(Long userId, Long skuId, Integer quantity);

    /**
     * 获取当前用户购物车中的所有 skuId 及对应数量
     */
    Map<Long, Integer> getCartList(Long userId);

    /**
     * 删除指定商品
     */
    void removeCartItem(Long userId, Long skuId);
}
