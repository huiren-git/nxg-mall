package com.nxg.mall.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券实体类
 */
@Data
public class Coupon {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    /**
     * 1-全场通用, 2-指定品类, 3-指定商品
     */
    private Integer category;

    /**
     * 1-满减, 2-折扣, 3-无门槛
     */
    private Integer type;

    private BigDecimal conditionAmount;

    private BigDecimal discountAmount;

    private BigDecimal discountRate;

    private Integer totalCount;

    private Integer takeCount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /**
     * 状态：0-已禁用, 1-已启用
     */
    private Integer status;

    /**
     * 逻辑删除：0-未删除, 1-已删除
     */
    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
