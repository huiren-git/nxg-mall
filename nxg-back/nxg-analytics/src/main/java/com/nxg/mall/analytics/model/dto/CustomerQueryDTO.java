package com.nxg.mall.analytics.model.dto;

import lombok.Data;

@Data
public class CustomerQueryDTO {
    private Integer current = 1;
    private Integer size = 10;
    private String nickname;
    private String sortField;
    private String sortOrder; // ascend | descend
}
