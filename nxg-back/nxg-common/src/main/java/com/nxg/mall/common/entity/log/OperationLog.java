package com.nxg.mall.common.entity.log;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

/**
 * 操作日志实体类
 */
@Data
@TableName("operation_log")
public class OperationLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 角色 (ADMIN/SALES) */
    private String role;

    /** 操作类型 (对应 ActionType 枚举的 code) */
    private String actionType;

    /** 对象类型 (对应 TargetType 枚举的 code) */
    private String targetType;

    /** 对象ID (如订单号、商品ID) */
    private String targetId;

    /** 对象名称 (如商品名称、用户昵称) */
    private String targetName;

    /** 详细描述/标题 */
    private String detail;

    /** 请求参数 */
    private String requestParams;

    /** 操作IP地址 */
    private String clientIp;

    /** 操作时间戳 */
    private Long opTime;
}