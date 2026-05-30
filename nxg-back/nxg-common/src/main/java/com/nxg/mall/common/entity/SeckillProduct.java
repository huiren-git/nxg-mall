package com.nxg.mall.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class SeckillProduct {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;
    private BigDecimal seckillPrice;
    private Integer stockCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
}
