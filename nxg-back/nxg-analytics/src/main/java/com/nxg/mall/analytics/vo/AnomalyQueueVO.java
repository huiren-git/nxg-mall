package com.nxg.mall.analytics.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AnomalyQueueVO {
    private Long id;

    @JsonFormat(pattern = "HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("time")
    private LocalDateTime createTime;

    @JsonProperty("type")
    private String anomalyType;

    @JsonProperty("target")
    private String targetObject;

    @JsonProperty("level")
    private String riskLevel;

    @JsonProperty("detail")
    private String description;
}