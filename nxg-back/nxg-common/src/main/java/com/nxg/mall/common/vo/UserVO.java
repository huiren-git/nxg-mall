package com.nxg.mall.common.vo;

import lombok.Data;

@Data
public class UserVO {
    private String nickname;
    private String avatar;
    private String role; // 可选：返回角色以供前端判断权限
}
