package com.nxg.mall.common.entity;

import lombok.Data;

@Data
public class CouponUser {
    private Long UserId;
    private Long CouponId;

    private Integer Status;
}
