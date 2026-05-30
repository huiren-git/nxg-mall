package com.nxg.mall.common.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ManageVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String role;
    private LocalDateTime createTime;

    // 从 t_user_status 表关联出的信息
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
}