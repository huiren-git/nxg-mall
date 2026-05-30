package com.nxg.mall.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.common.entity.AttributeValue;
import java.util.List;
import com.nxg.mall.common.dto.AttributeValueSaveDTO;

/**
 * 规格属性值服务接口
 */
public interface IAttributeValueService extends IService<AttributeValue> {

    /**
     * 根据属性名 ID 获取该维度下所有的可选值
     * * @param attrKeyId 属性名ID (如“颜色”的ID)
     * @return 属性值列表
     */
    List<AttributeValue> getValuesByKeyId(Long attrKeyId);

    /**
     * 批量保存或更新属性值
     * 用于在后台管理中，一次性为一个规格维度添加多个值
     */
    boolean saveOrUpdateBatchByField(List<AttributeValue> entityList);

    /**
     * 创建新的属性值
     * @param saveDTO
     * @return
     */
    AttributeValue saveByDTO(AttributeValueSaveDTO saveDTO);
}