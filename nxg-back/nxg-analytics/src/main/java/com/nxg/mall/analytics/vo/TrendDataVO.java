package com.nxg.mall.analytics.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TrendDataVO {
    private List<String> days;
    private List<Long> data;
    private String type; // visitors, newUsers, recharge
}
