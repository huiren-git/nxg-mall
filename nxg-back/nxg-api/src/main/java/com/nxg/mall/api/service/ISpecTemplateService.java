package com.nxg.mall.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.common.dto.SpecTemplateSaveDTO;
import com.nxg.mall.common.entity.SpecTemplate;
import com.nxg.mall.common.vo.SpecTemplateVO;

import java.util.List;

public interface ISpecTemplateService extends IService<SpecTemplate> {

    /**
     * 获取规格模板列表
     * @param name
     * @return
     */
    List<SpecTemplateVO> getComplexTemplateList(String name);

    void saveComplexTemplate(SpecTemplateSaveDTO dto);

    void removeComplexTemplate(List<Long> templateIds);

    void updateComplexTemplate(Long id, SpecTemplateSaveDTO dto);
}
