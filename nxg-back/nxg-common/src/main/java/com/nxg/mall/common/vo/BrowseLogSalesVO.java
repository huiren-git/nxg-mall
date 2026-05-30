package com.nxg.mall.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nxg.mall.common.enums.SourceTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BrowseLogSalesVO {
    private Long id;
    private Long productId;
    private String productName; // 需要关联商品表查询
    private String productImage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime enterTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime leaveTime;

    private Integer duration;
    private String clientIp;
    private String terminal;
    private SourceTypeEnum sourceType;
}
