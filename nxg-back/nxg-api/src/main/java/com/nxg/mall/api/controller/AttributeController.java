package com.nxg.mall.api.controller;

import com.nxg.mall.common.annotation.Log;
import com.nxg.mall.common.enums.ActionType;
import com.nxg.mall.common.enums.TargetType;
import com.nxg.mall.common.Result;
import com.nxg.mall.common.dto.AttributeValueSaveDTO;
import com.nxg.mall.common.dto.SpecTemplateSaveDTO;
import com.nxg.mall.common.entity.AttributeKey;
import com.nxg.mall.common.entity.AttributeValue;
import com.nxg.mall.api.mapper.AttributeKeyMapper;
import com.nxg.mall.api.service.IAttributeKeyService;
import com.nxg.mall.api.service.IAttributeValueService;
import com.nxg.mall.api.service.ISpecTemplateService;
import com.nxg.mall.api.service.ProductSpecService;
import com.nxg.mall.common.vo.ProductSpecVO;
import com.nxg.mall.common.vo.SpecTemplateVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/sales/attributes")
public class AttributeController {

    @Autowired
    private IAttributeKeyService attrKeyService;

    @Autowired
    private IAttributeValueService attrValueService;

    @Autowired
    private ISpecTemplateService specTemplateService;

    @Autowired
    private ProductSpecService productSpecService;

    @Autowired
    private AttributeKeyMapper attrKeyMapper;

    /**
     * 获取规格模板列表
     * GET /api/v1/attributes/templates?name=xxx
     */
    @GetMapping("/templates")
    public Result<List<SpecTemplateVO>> getTemplateList(
            @RequestParam(required = false) String name) {

        List<SpecTemplateVO> list = specTemplateService.getComplexTemplateList(name);
        return Result.success(list);
    }

    /**
     * 创建新的模板
     * @param dto
     * @return
     */
    @Log(title = "新增规格模板", actionType = ActionType.ADD_ATTRIBUTE, targetType = TargetType.ATTRIBUTE)
    @PostMapping("/templates")
    public Result<Void> saveTemplate(@Valid @RequestBody SpecTemplateSaveDTO dto) {
        specTemplateService.saveComplexTemplate(dto);
        return Result.success();
    }

    /**
     * 修改规格模板
     * PUT /api/v1/attributes/templates/{id}
     */
    @Log(title = "修改规格模板", actionType = ActionType.UPDATE_ATTRIBUTE, targetType = TargetType.ATTRIBUTE)
    @PreAuthorize("hasRole('SALES')")
    @PutMapping("/templates/{id}")
    public Result<Void> updateTemplate(@PathVariable Long id, @RequestBody SpecTemplateSaveDTO dto) {
        specTemplateService.updateComplexTemplate(id, dto);
        return Result.success();
    }

    /**
     * 单个删除模板
     * DELETE /api/v1/attributes/templates/{id}
     */
    @Log(title = "删除规格模板", actionType = ActionType.DELETE_ATTRIBUTE, targetType = TargetType.ATTRIBUTE)
    @PreAuthorize("hasRole('SALES')")
    @DeleteMapping("/templates/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        specTemplateService.removeComplexTemplate(Collections.singletonList(id));
        return Result.success();
    }

    /**
     * 批量删除模板
     * DELETE /api/v1/attributes/templates?ids=1,2,3
     */
    @Log(title = "批量删除规格模板", actionType = ActionType.DELETE_ATTRIBUTE, targetType = TargetType.ATTRIBUTE)
    @PreAuthorize("hasRole('SALES')")
    @DeleteMapping("/templates")
    public Result<Void> batchDeleteTemplates(@RequestParam List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的模板");
        }
        specTemplateService.removeComplexTemplate(ids);
        return Result.success();
    }

    /**
     * 管理属性名 (新增属性维度，如“颜色”)
     * POST /api/v1/attributes/key
     */
    @Log(title = "新增属性名", actionType = ActionType.ADD_ATTRIBUTE, targetType = TargetType.ATTRIBUTE)
    @PreAuthorize("hasRole('SALES')")
    @PostMapping("/keys")
    public Result<Void> saveAttrKey(@RequestBody AttributeKey attrKey) {
        attrKeyService.save(attrKey);
        return Result.success();
    }

    /**
     * 管理属性值 (新增可选值，如“曜石黑”)
     * POST /api/v1/attributes/value
     */
    @Log(title = "新增属性值", actionType = ActionType.ADD_ATTRIBUTE, targetType = TargetType.ATTRIBUTE)
    @PreAuthorize("hasRole('SALES')")
    @PostMapping("/values")
    public Result<AttributeValue> saveAttrValue(@RequestBody AttributeValueSaveDTO saveDTO) {
        AttributeValue entity = attrValueService.saveByDTO(saveDTO);
        return Result.success(entity);
    }
}
