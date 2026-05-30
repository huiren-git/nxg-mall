package com.nxg.mall.api.service;

import com.nxg.mall.common.vo.ProductSpecVO;

public interface ProductSpecService {
    /**
     * 构建商品规格选择器所需的所有数据
     * @param productId 商品ID
     * @return 包含规格元数据和SKU映射表的VO
     */
    ProductSpecVO buildSpecInfo(Long productId);
}
