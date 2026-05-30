package com.nxg.mall.api.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.api.mapper.AttributeKeyMapper;
import com.nxg.mall.common.entity.AttributeKey;
import com.nxg.mall.api.mapper.SpecTemplateMapper;
import com.nxg.mall.common.vo.ProductSpecVO;
import com.nxg.mall.api.service.IAttributeKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 规格属性名服务实现类
 */
@Service
public class AttributeKeyServiceImpl extends ServiceImpl<AttributeKeyMapper, AttributeKey> implements IAttributeKeyService {

    @Autowired
    private AttributeKeyMapper attributeKeyMapper;

    @Autowired
    private SpecTemplateMapper specTemplateMapper;
}
