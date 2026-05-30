package com.nxg.mall.analytics.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OperationLogQueryDTO {
    private String role;           // ALL, SALES, ADMIN
    private String keyword;        // 模糊搜索账号或内容
    private List<String> actionTypes; // 业务模块勾选
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer current = 1;
    private Integer pageSize = 10;
}
