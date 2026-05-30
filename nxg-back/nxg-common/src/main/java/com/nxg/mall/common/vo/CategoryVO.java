package com.nxg.mall.common.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryVO {
    private Long id;
    private String name;
    private Long parentId;
    private Integer level;

    private String path;

    private Integer sortOrder;

    private List<CategoryVO> children = new ArrayList<>();

}
