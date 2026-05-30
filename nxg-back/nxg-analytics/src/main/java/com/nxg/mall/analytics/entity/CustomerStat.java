package com.nxg.mall.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_customer_stat_wide")
public class CustomerStat {
    @TableId(type = IdType.INPUT)
    private Long userId;
    private String nickname;
    private String email;
    private String avatar;
    private BigDecimal balance;
    private Long orderCount;
    private BigDecimal totalSpend;
}
