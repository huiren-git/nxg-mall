package com.nxg.mall.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商品分析明细实体类
 * 对应数据库表：nxg_analytics.ads_product_inventory_analysis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ads_product_analysis")
public class ProductAnalysis {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品名称 (对应前端 columns: name)
     */
    private String productName;

    /**
     * 商品类别 (对应前端 filters: 数码家电, 户外运动等)
     */
    private String category;

    /**
     * 当前库存 (对应前端 columns: stock)
     */
    private Integer stock;

    /**
     * 商品状态 (对应前端 filters: 热销中, 库存预警, 正常)
     */
    private String status;

    /**
     * 商品销量 (对应前端 columns: salesVolume)
     */
    private Integer salesVolume;

    /**
     * 库存周转率，使用 BigDecimal 保证精度 (对应前端 Progress 进度条)
     */
    private BigDecimal turnoverRate;

    /**
     * 商品单价 (用于计算 ADS 层指标，不直接显示在表格中)
     */
    private BigDecimal price;

    /**
     * 数据更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 业务统计时间
     */
    private LocalDate dt;
}
