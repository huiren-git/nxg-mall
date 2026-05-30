package com.nxg.mall.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordDTO {
    @NotBlank(message = "非法的请求，Token缺失")
    private String token;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, message = "密码至少6位")
    private String newPassword;
}
