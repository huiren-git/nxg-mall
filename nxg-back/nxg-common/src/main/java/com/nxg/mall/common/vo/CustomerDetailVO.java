package com.nxg.mall.common.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CustomerDetailVO {
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private BigDecimal balance;

    // 登录轨迹
    private LocalDateTime registerTime;
    private LocalDateTime lastLoginTime;
    private String lastLoginArea;
    private String lastLoginIp;

    // 嵌套的用户画像对象
    private PersonaVO persona;

    @Data
    public static class PersonaVO {
        private String region;
        private String purchasingPower;
        private List<String> preferences; // 对应前端的数组
    }
}