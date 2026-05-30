package com.nxg.mall.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    private String account;

    @NotBlank
    private String password;
}
