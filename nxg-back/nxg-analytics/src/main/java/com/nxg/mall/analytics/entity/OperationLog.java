package com.nxg.mall.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String operatorAccount;
    private String operatorRole; // 存储 "SALES" 或 "ADMIN"
    private String targetType;      // 操作对象类型
    private String targetId;        // 操作对象ID
    private String targetName;      // 操作对象名称
    private String actionModule; // 对应前端 actionType/targetType
    private String actionContent; // 对应前端 detail
    private String ipAddress;
    private LocalDateTime createTime;
}
