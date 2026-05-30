package com.nxg.mall.analytics.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserPersonaVO {
    private Long userId;
    private String region;
    private String lastLoginArea;
    private String purchasingPower;
    private List<String> preferences; // 这里存中文名列表，如 ["咖啡", "数码"]
}
