package com.nxg.mall.common.vo;

import lombok.Data;

import java.util.List;

@Data
public class SpecTemplateVO {
    private Long id;                // 模板ID
    private String templateName;    // 模板名称
    private Long categoryId;        // 分类ID
    private String categoryName;    // 分类名称 (通过关联查询获得)
    private List<SpecKeyVO> specKeys; // 规格项列表

    @Data
    public static class SpecKeyVO {
        private Long id;
        private String attrName;     // 规格名：如“颜色”
        private List<SpecValueVO> attrValues; // 规格值列表
    }

    @Data
    public static class SpecValueVO {
        private Long id;
        private String attrValue;    // 规格值：如“红色”
    }
}
