package com.nxg.mall.analytics.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserPersonaDTO {
    private Long userId;
    private String region;
    private String purchasingPower;
    private String lastLoginArea;
    private List<String> preferences; // 对应前端 tags 展示
}
