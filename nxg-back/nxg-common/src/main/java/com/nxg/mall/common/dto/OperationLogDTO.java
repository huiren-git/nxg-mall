package com.nxg.mall.common.dto;

import com.nxg.mall.common.enums.ActionType;
import com.nxg.mall.common.enums.TargetType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// 操作日志 DTO
@Data
public class OperationLogDTO {
    @NotBlank(message = "操作类型不能为空")
    private ActionType actionType;    // 如：MODIFY_PRICE, BAN_USER

    @NotBlank(message = "对象类型不能为空")
    private TargetType targetType;    // 如：ORDER, PRODUCT, USER

    @NotBlank(message = "对象ID不能为空")
    private String targetId;      // 如：ORD20260511001

    private String targetName;    // 辅助描述：iPhone 15
    private String detail;        // 详细描述
}
