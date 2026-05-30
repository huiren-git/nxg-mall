package com.nxg.mall.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CaptchaScene {

    REGISTER("register", "用户注册"),
    UPDATE_PASSWORD("update_pwd", "修改密码"),
    UPDATE_EMAIL_OLD("email_old", "验证旧邮箱"),
    UPDATE_EMAIL_NEW("email_new", "验证新邮箱");

    private final String code;
    private final String description;

    // 根据 code 获取枚举，方便 Controller 层校验
    public static CaptchaScene getByCode(String code) {
        for (CaptchaScene scene : values()) {
            if (scene.code.equals(code)) {
                return scene;
            }
        }
        return null;
    }
}