package com.nxg.mall.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class User {

    @TableId(type = IdType.AUTO)   // 主键自增
    private Long id;

    private String username;
    private String password;
    private String email;

    // 用户角色 0-Customer 1-Sales 2-Admin
    private Integer role = 0;

    private String nickname;
    private String avatar;

    /**
     * 账户余额
     * 使用 BigDecimal 防止精度丢失
     */
    private BigDecimal balance;


    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}