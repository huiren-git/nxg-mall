package com.nxg.mall.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.common.vo.SeckillProductVO;
import com.nxg.mall.common.entity.Product;
import com.nxg.mall.common.entity.SeckillProduct;
import com.nxg.mall.api.mapper.SeckillProductMapper;
import com.nxg.mall.api.service.IProductService;
import com.nxg.mall.api.service.ISeckillService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SeckillServiceImpl extends ServiceImpl<SeckillProductMapper, SeckillProduct> implements ISeckillService {

    @Value("${file.domain}")
    private String domain;

    @Autowired
    private IProductService productService;

    @Override
    public List<SeckillProductVO> getActiveSeckillProducts() {
        // 1. 查询当前时间处于 [开始之前] 或 [进行中] 的秒杀配置
        LocalDateTime now = LocalDateTime.now();
        List<SeckillProduct> seckillConfigs = this.list(new LambdaQueryWrapper<SeckillProduct>()
                .gt(SeckillProduct::getEndTime, now) // 还没结束的
                .orderByAsc(SeckillProduct::getStartTime));

        if (seckillConfigs.isEmpty()) return Collections.emptyList();

        // 2. 提取所有关联的商品 ID
        List<Long> productIds = seckillConfigs.stream()
                .map(SeckillProduct::getProductId)
                .collect(Collectors.toList());

        // 3. 批量查询商品基础信息 (避免在循环里查数据库)
        Map<Long, Product> productMap = productService.listByIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 4. 组装 VO
        return seckillConfigs.stream().map(config -> {
            Product p = productMap.get(config.getProductId());
            SeckillProductVO vo = new SeckillProductVO();

            // 复制秒杀特有属性
            BeanUtils.copyProperties(config, vo);

            // 补充商品基础属性
            if (p != null) {
                vo.setName(p.getName());
                vo.setMainImage(p.getMainImage());
                vo.setFullImageUrl(domain + p.getMainImage());
                vo.setOriginalPrice(p.getPrice());
            }

            // 动态判断状态
            if (now.isBefore(config.getStartTime())) {
                vo.setStatus(0); // 预热中
            } else {
                vo.setStatus(1); // 进行中
            }

            return vo;
        }).collect(Collectors.toList());
    }
}
