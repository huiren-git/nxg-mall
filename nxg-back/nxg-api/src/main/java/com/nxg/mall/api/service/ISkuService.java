package com.nxg.mall.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.common.dto.SkuDTO;
import com.nxg.mall.common.entity.Sku;
import com.nxg.mall.common.vo.SkuVO;

import java.util.List;

public interface ISkuService extends IService<Sku> {

    /**
     * 为指定商品保存 SKU 列表
     */
    void saveSkuList(Long productId, List<SkuDTO> skuDtoList, Long operatorId);

    /**
     * 获取指定商品的sku列表
     * @param productId
     * @return
     */
    List<SkuVO> getSkuListByProductId(Long productId);

    String generateSkuCode(Long productId, int sequence);
}
