package com.nxg.mall.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.common.dto.AttributeValueSaveDTO;
import com.nxg.mall.api.mapper.AttributeValueMapper;
import com.nxg.mall.common.entity.AttributeValue;
import com.nxg.mall.api.service.IAttributeValueService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 规格属性值服务实现类
 */
@Service
public class AttributeValueServiceImpl extends ServiceImpl<AttributeValueMapper, AttributeValue> implements IAttributeValueService {

    @Override
    public List<AttributeValue> getValuesByKeyId(Long attrKeyId) {
        // 使用 MyBatis-Plus 的 Lambda 查询，增加可读性并防止字段名写错
        return this.list(new LambdaQueryWrapper<AttributeValue>()
                .eq(AttributeValue::getAttrKeyId, attrKeyId)
                .eq(AttributeValue::getIsDeleted, 0) // 仅查询未删除的
                .orderByAsc(AttributeValue::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateBatchByField(List<AttributeValue> entityList) {
        // 利用 ServiceImpl 提供的 saveOrUpdateBatch 快速实现批量操作
        return this.saveOrUpdateBatch(entityList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttributeValue saveByDTO(AttributeValueSaveDTO saveDTO) {
        AttributeValue entity = new AttributeValue();
        BeanUtils.copyProperties(saveDTO, entity);

        this.save(entity);
        // 保存后，MyBatis-Plus 会自动将自增 ID 回填到 entity 对象中
        return entity;
    }
}
