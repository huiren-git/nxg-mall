package com.nxg.mall.analytics.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user_persona")
public class UserPersona {
    @TableId(type = IdType.INPUT) // 因为 ID 是从 nxg-api 同步过来的，不是自增
    private Long userId;

    private String region;

    private String purchasingPower;

    private String tags; // 数据库存储为 "数码,咖啡" 格式

    private String lastLoginArea;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
