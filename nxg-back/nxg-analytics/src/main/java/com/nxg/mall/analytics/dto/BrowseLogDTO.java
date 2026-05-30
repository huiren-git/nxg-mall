package com.nxg.mall.analytics.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowseLogDTO {

    @NotNull(message = "商品ID不能为空")
    private String productId; // 👈 改为 String，完美兼容前端的 "3"

    private Long categoryId;  // 前端传的是 14（数字），不用改

    @NotNull(message = "进入时间不能为空")
    private String enterTime; // 👈 改为 String，防止 Jackson 时间组件缺失导致崩溃

    @NotNull(message = "离开时间不能为空")
    private String leaveTime; // 👈 改为 String

    private Integer duration;
    private String terminal;
    private String sourceType;
}
