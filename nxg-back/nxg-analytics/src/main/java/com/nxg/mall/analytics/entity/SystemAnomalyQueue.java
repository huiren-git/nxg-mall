package com.nxg.mall.analytics.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_system_anomaly_queue")
public class SystemAnomalyQueue {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 业务域：1-订单域, 2-支付域, 3-库存域, 4-用户域
     */
    private Integer businessType;

    /**
     * 异常类型（如：支付异常、大额待审、库存预警）
     */
    private String anomalyType;

    /**
     * 目标主体对象（如订单号、SKU编码、用户ID）
     */
    private String targetObject;

    /**
     * 关联买家用户名/操作人（新增）
     */
    private String buyerName;

    /**
     * 关联订单/交易金额（新增）
     */
    private BigDecimal orderAmount;

    /**
     * 风险等级（高危、中危、紧急、低危）
     */
    private String riskLevel;

    /**
     * 异常触发的上下文辅助描述（已优化为 Varchar）
     */
    private String description;

    /**
     * 处理状态：0-未处理，1-已处理
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}