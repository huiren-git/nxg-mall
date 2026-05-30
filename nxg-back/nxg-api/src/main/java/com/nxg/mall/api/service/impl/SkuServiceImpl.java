package com.nxg.mall.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.common.dto.SkuDTO;
import com.nxg.mall.common.entity.Sku;
import com.nxg.mall.api.mapper.SkuMapper;
import com.nxg.mall.api.service.ISkuService;
import com.nxg.mall.common.vo.SkuVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class SkuServiceImpl extends ServiceImpl<SkuMapper, Sku> implements ISkuService {

    @Value("${file.domain}")
    private String domain;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSkuList(Long productId, List<SkuDTO> skuDtoList, Long operatorId) {
        if (CollectionUtils.isEmpty(skuDtoList)) {
            return;
        }

        AtomicInteger sequence = new AtomicInteger(1);

        // 1. 将 DTO 转换为 Entity
        List<Sku> skus = skuDtoList.stream().map(dto -> {
            Sku sku = new Sku();
            BeanUtils.copyProperties(dto, sku);

            sku.setProductId(productId);
            sku.setOperatorId(operatorId);
            sku.setStatus(1);
            String generatedCode = generateSkuCode(productId, sequence.getAndIncrement());
            sku.setSkuCode(generatedCode);

            return sku;
        }).collect(Collectors.toList());

        // 2. 直接调用 IService 提供的 saveBatch 方法
        // 默认每 1000 条数据执行一次真正的数据库批处理提交
        this.saveBatch(skus);
    }

    @Override
    public List<SkuVO> getSkuListByProductId(Long productId) {
        LambdaQueryWrapper<Sku> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Sku::getProductId, productId)
                .orderByAsc(Sku::getId);

        List<Sku> skus = this.list(queryWrapper);

        return skus.stream().map(sku -> {
            SkuVO vo = new SkuVO();
            BeanUtils.copyProperties(sku, vo);

            // 核心逻辑：拼接图片全路径
            // 注意判断 MainImage 是否为空，避免拼接出 "http://domain.com/null"
            if (StringUtils.hasText(sku.getMainImage())) {
                // 确保 domain 以 / 结尾或者路径以 / 开头，防止拼接错误
                String fullPath = domain.endsWith("/") ? domain : domain + "/";
                String imagePath = sku.getMainImage().startsWith("/") ? sku.getMainImage().substring(1) : sku.getMainImage();
                vo.setMainImage(fullPath + imagePath);
            }

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public String generateSkuCode(Long productId, int sequence) {
        // 示例：P1024S001, P1024S002...
        return String.format("P%dS%03d", productId, sequence);
    }
}
