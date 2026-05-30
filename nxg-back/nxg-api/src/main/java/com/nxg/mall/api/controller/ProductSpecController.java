package com.nxg.mall.api.controller;


import com.nxg.mall.common.Result;
import com.nxg.mall.api.service.ProductSpecService;
import com.nxg.mall.common.vo.ProductSpecVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductSpecController {

    @Autowired
    private ProductSpecService specService;

    @GetMapping("/{id}/spec-info")
    public Result<ProductSpecVO> getProductSpecInfo(@PathVariable("id") Long productId) {
        ProductSpecVO specInfo = specService.buildSpecInfo(productId);
        return Result.success(specInfo);
    }
}
