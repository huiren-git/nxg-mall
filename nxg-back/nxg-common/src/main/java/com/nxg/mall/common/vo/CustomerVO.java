package com.nxg.mall.common.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CustomerVO {
    private Long id;
    private String nickname;
    private String email;
    private BigDecimal balance;
    private Integer orderCount;   // 购买商品数
    private BigDecimal totalSpent; // 累计消费金额
    private Integer status;       // 账户状态
    private LocalDateTime createdAt; // 注册时间
}
