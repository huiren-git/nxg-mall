package com.nxg.mall.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nxg.mall.api.mapper.*;
import com.nxg.mall.common.entity.*;
import com.nxg.mall.api.service.ProductSpecService;
import com.nxg.mall.common.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductSpecServiceImpl implements ProductSpecService {

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private AttributeKeyMapper attrKeyMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public ProductSpecVO buildSpecInfo(Long productId) {
        // 1. 获取该商品关联的所有规格维度及其可选值
        // 逻辑：通过 t_product_attr_config 找到该商品开启的 key，再找对应的 values
        List<ProductSpecVO.SpecKeyVO> specKeys = attrKeyMapper.selectSpecMetadataByProductId(productId);

        // 2. 查询该商品下所有上架状态的 SKU
        List<Sku> skuList = skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getProductId, productId)
                .eq(Sku::getStatus, 1) // 1-上架
                .eq(Sku::getIsDeleted, 0));

        // 3. 构建 skuMap: 将规格组合字符串映射到具体 SKU 信息
        Map<String, ProductSpecVO.SkuInfoVO> skuMap = new HashMap<>();

        for (Sku sku : skuList) {
            // 解析 specs 字段: {"颜色":"黑色", "内存":"256G"}
            try{
                Map<String, String> specItems = objectMapper.readValue(
                        sku.getSpecs(),
                        new TypeReference<Map<String, String>>() {}
                );

                // 【核心算法】：对规格值进行排序并拼接
                // 为什么要排序？因为前端点击顺序不确定，必须有一个标准 key（如 "256G,黑色"）
                String mapKey = specItems.values().stream()
                        .sorted()
                        .collect(Collectors.joining(","));

                ProductSpecVO.SkuInfoVO info = new ProductSpecVO.SkuInfoVO();
                info.setSkuId(sku.getId());
                info.setPrice(sku.getPrice());
                info.setStock(sku.getStock());
                info.setSkuCode(sku.getSkuCode());
                info.setMainImage(sku.getMainImage());

                skuMap.put(mapKey, info);
            } catch (Exception e) {
                // 处理解析异常，根据业务决定是跳过还是抛错
                System.out.println("解析SKU规格失败, skuId: {}" + sku.getId() + e);
                continue;
            }


        }

        // 4. 组装最终结果
        ProductSpecVO vo = new ProductSpecVO();
        vo.setSpecKeys(specKeys);
        vo.setSkuMap(skuMap);

        return vo;
    }
}
