package com.nxg.mall.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CustomerStatDaily {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private Long uvCount;
    private Long pvCount;
    private Integer newUserCount;
    private Integer tradeUserCount;
    private Integer rechargeUserCount;
}
