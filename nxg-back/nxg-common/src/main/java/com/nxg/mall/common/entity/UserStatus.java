package com.nxg.mall.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserStatus {
    @TableId(type = IdType.INPUT)   // 使用 user_id 作为主键，手动赋值
    private Long userId;            // 用户ID，关联 user 表

    private Integer status;         // 账户状态: 0-冻结, 1-正常 (默认1)

    private Integer freezeReason;   // 冻结原因: 0-申请注销，1-违规

    private LocalDateTime freezeExpireTime; // 冻结到期时间

    private LocalDateTime logoutApplyTime;  // 申请注销时间

    private LocalDateTime deleteTime;       // 预计物理删除时间

    private LocalDateTime lastLoginTime;    // 最近登录成功的时间

    private String lastLoginIp;             // 最近登录成功的 IP
}