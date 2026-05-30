package com.nxg.mall.analytics.vo;

import lombok.Data;

@Data
public class AnomalyLogVO {
    private String id;       // 对应前端的 id，我们传 logNo
    private String time;     // 格式化后的时间
    private String type;     // anomalyType
    private String target;   // targetObj
    private String level;    // riskLevel
    private String detail;   // detailDesc
}
