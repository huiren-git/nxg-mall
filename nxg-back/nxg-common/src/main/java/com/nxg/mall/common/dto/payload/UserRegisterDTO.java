package com.nxg.mall.common.dto.payload;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserRegisterDTO {
    private Long userId;
    private String username;
    private String email;
    private String registerIp;
    private String terminal; // 如 "Web", "App"
    private Long timestamp;
}
