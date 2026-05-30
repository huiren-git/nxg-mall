package com.nxg.mall.common.vo;

import lombok.Data;

@Data
public class LoginResponseVO {
    private String token;
    private UserCenterInfoVO userInfo;
}