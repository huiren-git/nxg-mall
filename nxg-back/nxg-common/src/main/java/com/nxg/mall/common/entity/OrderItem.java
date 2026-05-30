package com.nxg.mall.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private Long productId;

    private Long skuId;
    private String specs;

    private BigDecimal price;  // 原单价
    private Integer quantity;

    // 商品快照
    private String productName;
    private String productPic;

    private Integer isPaid;            // 支付状态: 0-未付, 1-已付

    /**
     * 该明细最终应付/实付总金额
     * 在无优惠情况下：payAmount = price * quantity
     */
    private BigDecimal payAmount = BigDecimal.ZERO;

    // 优惠券相关
    // private Long categoryId; // 用于判断是否符合品类券
    // 每一层的减免金额
    private BigDecimal couponDiscountAmount;
    private BigDecimal realAmount;

    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private Integer productStock;
}
