package com.nxg.mall.common.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillProductVO {
    private Long id;            // 秒杀活动ID
    private Long productId;     // 商品ID
    private String name;        // 商品名称
    private String mainImage;   // 商品主图
    private String fullImageUrl; // 全路径图
    private BigDecimal originalPrice; // 原价 (来自 t_product)
    private BigDecimal seckillPrice;  // 秒杀价 (来自 t_seckill_product)
    private Integer stockCount;       // 秒杀剩余库存
    private LocalDateTime startTime;  // 开始时间 (用于前端倒计时)
    private LocalDateTime endTime;    // 结束时间
    private Integer status;           // 状态：0-预热，1-进行中
}
