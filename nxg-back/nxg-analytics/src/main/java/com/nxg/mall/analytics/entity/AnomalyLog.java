package com.nxg.mall.analytics.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_anomaly_log")
public class AnomalyLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String logNo;
    private String anomalyType;
    private String targetObj;
    private String riskLevel;
    private String detailDesc;

    /**
     * 处理状态: 0-待处理, 1-已拦截, 2-已忽略
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
