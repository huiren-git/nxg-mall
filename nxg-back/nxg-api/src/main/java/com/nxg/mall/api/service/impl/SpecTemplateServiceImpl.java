package com.nxg.mall.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.api.service.IAttributeKeyService;
import com.nxg.mall.api.service.IAttributeValueService;
import com.nxg.mall.common.dto.SpecTemplateSaveDTO;
import com.nxg.mall.common.entity.AttributeKey;
import com.nxg.mall.common.entity.AttributeValue;
import com.nxg.mall.common.entity.SpecTemplate;
import com.nxg.mall.api.mapper.AttributeKeyMapper;
import com.nxg.mall.api.service.IAttributeValueService;
import com.nxg.mall.api.mapper.SpecTemplateMapper;
import com.nxg.mall.api.service.ISpecTemplateService;
import com.nxg.mall.common.vo.SpecTemplateVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SpecTemplateServiceImpl extends ServiceImpl<SpecTemplateMapper, SpecTemplate> implements ISpecTemplateService {

    @Autowired
    private SpecTemplateMapper specTemplateMapper;

    @Autowired
    private IAttributeKeyService attributeKeyService;

    @Autowired
    private IAttributeValueService attributeValueService;

    @Override
    public List<SpecTemplateVO> getComplexTemplateList(String name) {
        // 直接一行代码搞定，性能由于是单次 SQL JOIN 查询，会比循环快很多
        return specTemplateMapper.selectComplexList(name);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveComplexTemplate(SpecTemplateSaveDTO dto) {
        // 1. 保存模板主表
        SpecTemplate template = new SpecTemplate();
        template.setTemplateName(dto.getTemplateName());
        template.setCategoryId(dto.getCategoryId());
        this.baseMapper.insert(template);

        // 2. 调用拆分出的私有方法保存关联数据
        saveItemsAndValues(template.getId(), dto.getSpecList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateComplexTemplate(Long id, SpecTemplateSaveDTO dto) {
        // 1. 更新模板主表信息
        SpecTemplate template = this.getById(id);
        if (template == null) throw new RuntimeException("模板不存在");

        template.setTemplateName(dto.getTemplateName());
        template.setCategoryId(dto.getCategoryId());
        this.updateById(template);

        // 2. 清理旧数据 (物理删除或逻辑删除)
        // 找到该模板下所有的 keyId
        List<AttributeKey> oldKeys = attributeKeyService.list(new LambdaQueryWrapper<AttributeKey>()
                .eq(AttributeKey::getTemplateId, id));

        if (!oldKeys.isEmpty()) {
            List<Long> oldKeyIds = oldKeys.stream().map(AttributeKey::getId).collect(Collectors.toList());
            // 删除旧的 values 和 keys
            attributeValueService.remove(new LambdaQueryWrapper<AttributeValue>().in(AttributeValue::getAttrKeyId, oldKeyIds));
            attributeKeyService.remove(new LambdaQueryWrapper<AttributeKey>().eq(AttributeKey::getTemplateId, id));
        }

        // 3. 调用之前的保存逻辑，重新插入 (复用之前写的批量插入代码)
        saveItemsAndValues(id, dto.getSpecList());
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeComplexTemplate(List<Long> templateIds) {
        // 1. 逻辑删除模板主表 (MyBatis-Plus 自动处理 is_deleted = 1)
        this.removeByIds(templateIds);

        // 2. 查找这些模板关联的所有 Key ID
        List<AttributeKey> keys = attributeKeyService.list(new LambdaQueryWrapper<AttributeKey>()
                .in(AttributeKey::getTemplateId, templateIds));

        if (keys.isEmpty()) return;

        List<Long> keyIds = keys.stream().map(AttributeKey::getId).collect(Collectors.toList());

        // 3. 逻辑删除规格项 (t_attribute_key)
        // 注意：如果 AttributeKey 类没有配 @TableLogic，这里会物理删除
        UpdateWrapper<AttributeKey> keyUpdate = new UpdateWrapper<>();
        keyUpdate.in("id", keyIds).set("is_deleted", 1);
        attributeKeyService.update(null, keyUpdate);

        // 4. 逻辑删除规格值 (t_attribute_value)
        UpdateWrapper<AttributeValue> valueUpdate = new UpdateWrapper<>();
        valueUpdate.in("attr_key_id", keyIds).set("is_deleted", 1);
        attributeValueService.update(null, valueUpdate);
    }

    /**
     * 核心复用逻辑：保存规格项及其对应的规格值
     * @param templateId 关联的模板ID
     * @param specList 前端传来的规格列表
     */
    private void saveItemsAndValues(Long templateId, List<SpecTemplateSaveDTO.SpecItemDTO> specList) {
        if (specList == null || specList.isEmpty()) return;

        // 1. 准备并批量插入 Keys
        List<AttributeKey> keys = specList.stream().map(item -> {
            AttributeKey key = new AttributeKey();
            key.setTemplateId(templateId);
            key.setAttrName(item.getAttrName());
            return key;
        }).collect(Collectors.toList());

        // 利用 useGeneratedKeys 回填 ID
        attributeKeyService.saveBatch(keys);

        // 2. 汇总并准备所有的 Values
        List<AttributeValue> allValues = new ArrayList<>();

        for (int i = 0; i < specList.size(); i++) {
            SpecTemplateSaveDTO.SpecItemDTO itemDTO = specList.get(i);
            Long keyId = keys.get(i).getId(); // 获取 insertBatch 回填后的 ID

            List<String> valNames = Arrays.stream(itemDTO.getAttrValues().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .collect(Collectors.toList());

            for (String valName : valNames) {
                AttributeValue av = new AttributeValue();
                av.setAttrKeyId(keyId);
                av.setAttrValue(valName);
                allValues.add(av);
            }
        }

        // 3. 一次性批量插入所有规格值
        if (!allValues.isEmpty()) {
            // 这里假设你已经注入了 iAttributeValueService
            attributeValueService.saveBatch(allValues);
        }
    }
}
