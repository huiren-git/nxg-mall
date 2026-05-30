package com.nxg.mall.api.controller;

import com.nxg.mall.common.Result;
import com.nxg.mall.api.service.ISkuService;
import com.nxg.mall.common.vo.SkuVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
public class SkuController {

    @Autowired
    private ISkuService skuService;

    /**
     * 获取指定商品下的所有 SKU
     * GET /api/v1/products/{productId}/skus
     */
    @GetMapping("/{productId}/skus")
    public Result<List<SkuVO>> getSkusByProductId(@PathVariable Long productId) {
        // 逻辑处理委派给 Service 层
        List<SkuVO> skuList = skuService.getSkuListByProductId(productId);
        return Result.success(skuList);
    }
}
