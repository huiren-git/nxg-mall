package com.nxg.mall.common.dto;

import lombok.Data;

@Data
public class CustomerQueryDTO {
    private Integer page = 1;
    private Integer size = 10;
    private String nickname;
    private String email;
    private Integer status;
    private String startTime;
    private String endTime;
}
