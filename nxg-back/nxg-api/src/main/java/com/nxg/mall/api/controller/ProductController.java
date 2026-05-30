package com.nxg.mall.api.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.nxg.mall.api.vo.ProductDetailManageVO;
import com.nxg.mall.common.Result;
import com.nxg.mall.common.dto.ProductCustomerQueryDTO;
import com.nxg.mall.common.dto.ProductManageQueryDTO;
import com.nxg.mall.common.dto.ProductUpdateDTO;
import com.nxg.mall.common.dto.ProductCreateDTO;
import com.nxg.mall.common.entity.Product;
import com.nxg.mall.common.utils.SecurityUtils;
import com.nxg.mall.common.vo.ProductDetailVO;
import com.nxg.mall.api.service.IProductService;
import com.nxg.mall.common.vo.ProductListVO;
import com.nxg.mall.common.vo.ProductManageVO;
import jakarta.validation.Valid;
import com.nxg.mall.common.annotation.Log;
import com.nxg.mall.common.enums.ActionType;
import com.nxg.mall.common.enums.TargetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class ProductController {

    @Autowired
    private IProductService productService;

    @PostMapping("/products")
    public Result<Void> createProduct(@RequestBody @Valid ProductCreateDTO createDTO) {
        // 假设从安全上下文获取当前操作人ID
        Long operatorId = 1L;
        productService.createProduct(createDTO, operatorId);
        return Result.success();
    }

    @GetMapping("/products/{id}")
    public Result<ProductDetailVO> getProductDetail(@PathVariable("id") Long id) {
        ProductDetailVO detail = productService.getProductDetail(id);
        return Result.success(detail);
    }

    @GetMapping("/sales/products/{id}")
    public Result<ProductDetailManageVO> getProductDetailManage(@PathVariable("id") Long id) {
        ProductDetailManageVO detail = productService.getProductDetailManage(id);
        return Result.success(detail);
    }

    @GetMapping("/products/{id}/related")
    public Result<List<ProductListVO>> getRelatedProducts(@PathVariable("id") Long productId) {
        List<ProductListVO> relatedProducts = productService.getRelatedProducts(productId);
        return Result.success(relatedProducts);
    }

    @GetMapping("/products/home/recommend")
    public Result<List<ProductListVO>> getHomeRecommendProducts() {
        Long userId = SecurityUtils.getUserId();
        List<ProductListVO> recommendProducts = productService.getHomeRecommendProducts(userId);
        return Result.success(recommendProducts);
    }

    @GetMapping("/products")
    public Result<IPage<ProductListVO>> list(ProductCustomerQueryDTO queryDTO) {
        Long userId = SecurityUtils.getUserId();
        // 调用业务层分页查询
        IPage<ProductListVO> pageResult = productService.getProductPage(queryDTO, userId);
        return Result.success(pageResult);
    }



    @GetMapping("/sales/products")
    public Result<IPage<ProductManageVO>> list(ProductManageQueryDTO query) {
        return Result.success(productService.getManageProductPage(query));
    }

    // 修改商品上下架状态
    @PutMapping("/sales/products/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam Boolean isOnShelf) {

        productService.updateShelfStatus(id, isOnShelf);
        return Result.success();
    }

    /**
     * 移入回收站 (逻辑删除)
     * DELETE /api/...
     */
    @Log(title = "商品移入回收站", actionType = ActionType.DELETE_PRODUCT, targetType = TargetType.PRODUCT)
    @PreAuthorize("hasRole('SALES')")
    @DeleteMapping("/sales/products/{id}")
    public Result<Void> moveToTrash(@PathVariable Long id) {
        // 因为配置了 @TableLogic，这里实际执行的是 SQL UPDATE
        productService.removeById(id);
        return Result.success();
    }

    /**
     * 从回收站还原商品
     * PUT
     */
    @Log(title = "从回收站恢复商品", actionType = ActionType.UPDATE_PRODUCT, targetType = TargetType.PRODUCT)
    @PreAuthorize("hasRole('SALES')")
    @PutMapping("sales/products/{id}/restore")
    public Result<Void> restoreProduct(@PathVariable Long id) {
        productService.restoreProduct(id);
        return Result.success();
    }

    /**
     * 修改商品信息
     * PUT
     */
    @Log(title = "修改商品信息", actionType = ActionType.UPDATE_PRODUCT, targetType = TargetType.PRODUCT)
    @PutMapping("/sales/products/{id}")
    public Result<Void> updateProduct(
            @PathVariable Long id,
            @Validated @RequestBody ProductUpdateDTO updateDTO) {

        productService.updateProductInfo(id, updateDTO);
        return Result.success();
    }
}
