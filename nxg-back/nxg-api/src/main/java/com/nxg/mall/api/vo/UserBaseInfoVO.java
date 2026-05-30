package com.nxg.mall.api.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserBaseInfoVO {
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private BigDecimal balance;
    private LocalDateTime registerTime; // 对应 create_time
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
}
