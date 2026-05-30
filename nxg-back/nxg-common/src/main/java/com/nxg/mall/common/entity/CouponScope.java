package com.nxg.mall.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class CouponScope {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long couponId;
    private Long targetId;   // 对应的商品ID或品类ID
}
