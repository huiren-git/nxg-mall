package com.nxg.mall.common.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class CouponQueryDTO {
    private Integer current = 1;
    private Integer size = 10;

    // 领券中心可能需要的过滤：1-全场, 2-品类, 3-单品
    private Integer category;
}