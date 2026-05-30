package com.nxg.mall.api.controller;

import com.nxg.mall.common.Result;
import com.nxg.mall.common.annotation.Log;
import com.nxg.mall.common.dto.CartAllCheckedDTO;
import com.nxg.mall.common.dto.CartDTO;
import com.nxg.mall.common.dto.CartUpdateDTO;
import com.nxg.mall.common.vo.CartVO;
import com.nxg.mall.api.service.ICartService;
import com.nxg.mall.common.utils.JwtUtil;
import com.nxg.mall.common.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customer/cart")
public class CartController {

    @Autowired
    private ICartService cartService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private HttpServletRequest request;

    @PostMapping("/add")
    public Result<Void> addToCart(@RequestBody CartDTO cartDTO) {
        // 从当前登录上下文（如 JWT/Session）获取 userId
        Long currentUserId = SecurityUtils.getUserId();
        // 从 DTO 中获取参数
        cartService.addToCart(currentUserId, cartDTO.getSkuId(), cartDTO.getQuantity());
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<CartVO>> getCartList() {
        return Result.success(cartService.listMyCart(SecurityUtils.getUserId()));
    }

    /**
     * 获取当前登录用户的购物车商品种类数量
     * GET /api/v1/cart/count
     */
    @GetMapping("/count")
    public Result<Integer> getCartCount() {
        // 从 SecurityContext 或拦截器中获取当前用户 ID
        Long userId = SecurityUtils.getUserId();

        Integer count = cartService.getCartCountByUserId(userId);
        return Result.success(count);
    }

    // 1. 修改数量
    @PutMapping("/update")
    public Result<Void> updateCartItem(@RequestBody CartUpdateDTO updateDto) {
        cartService.updateCartItem(
                SecurityUtils.getUserId(),
                updateDto
        );
        return Result.success();
    }

    // 3. 批量删除
    @DeleteMapping("/delete")
    public Result<Void> deleteItems(@RequestBody List<Long> skuIds) {
        cartService.deleteItems(SecurityUtils.getUserId(), skuIds);
        return Result.success();
    }

    // 4. 全选/取消全选
    @PutMapping("/update/allChecked")
    public Result<String> updateAllChecked(@RequestBody CartAllCheckedDTO allCheckedDto) {
        Long userId = SecurityUtils.getUserId();
        cartService.updateAllCheckedStatus(userId, allCheckedDto.getChecked());
        return Result.success("操作成功");
    }
}
