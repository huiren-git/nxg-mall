package com.nxg.mall.common.vo;

import lombok.Data;

@Data
public class UserCenterInfoVO {
    private Long userId;
    private String username;
    private String nickname;      // 可为空，暂用 username 替代
    private String avatar;        // 可为空
    private String role;   // 角色
    private String status;       // 账户状态 0-冻结 1-正常
}