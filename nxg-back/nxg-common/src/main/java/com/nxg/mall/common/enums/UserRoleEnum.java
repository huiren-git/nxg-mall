package com.nxg.mall.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserRoleEnum {
    CUSTOMER(0, "ROLE_CUSTOMER", "普通客户"),
    SALES(1, "ROLE_SALES", "销售人员"),
    ADMIN(2, "ROLE_ADMIN", "系统管理员");

    private final Integer code;
    private final String roleName;
    private final String description;

    public static UserRoleEnum getByCode(Integer code) {
        for (UserRoleEnum value : UserRoleEnum.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return CUSTOMER; // 默认角色
    }
}
