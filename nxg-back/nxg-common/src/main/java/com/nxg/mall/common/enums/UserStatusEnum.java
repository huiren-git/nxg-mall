package com.nxg.mall.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatusEnum {
    FROZEN(0, "FROZEN", "已冻结"),
    ACTIVE(1, "ACTIVE", "活跃");

    private final Integer code;
    private final String value;
    private final String description;

    public static UserStatusEnum getByCode(Integer code) {
        if (code == null) return null;
        for (UserStatusEnum status : UserStatusEnum.values()) {
            if (status.getCode().equals(code)) return status;
        }
        return null;
    }
}
