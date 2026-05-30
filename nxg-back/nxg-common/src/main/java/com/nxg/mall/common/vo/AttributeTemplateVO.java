package com.nxg.mall.common.vo;

import lombok.Data;

import java.util.List;

@Data
public class AttributeTemplateVO {
    private Long id;              // AttributeKey 的 ID
    private String attrName;      // 属性名：如“颜色”
    private Long categoryId;      // 所属分类
    private List<ValueVO> attrValues; // 关联的属性值列表

    @Data
    public static class ValueVO {
        private Long id;
        private String attrValue; // 属性值：如“红色”
    }
}
