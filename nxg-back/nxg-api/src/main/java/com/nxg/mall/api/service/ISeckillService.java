package com.nxg.mall.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.common.vo.SeckillProductVO;
import com.nxg.mall.common.entity.SeckillProduct;

import java.util.List;

public interface ISeckillService extends IService<SeckillProduct> {
    List<SeckillProductVO> getActiveSeckillProducts();
}
