package com.nxg.mall.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;    // 商品总原价
    private BigDecimal payAmount;      // 最终总应付

    private String remark;             // 订单备注

    private Long addressId;

    // 收货信息快照
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;

    private Integer status;            // 状态 0-待支付, 1-待发货, 2-待收货, 3-待评价, 4-已完成, 5-已取消

    private LocalDateTime payTime; // 支付时间
    private Integer payType;       // 支付类型：1-余额，2-支付宝，3-微信，4-混合支付

    @TableField(fill = FieldFill.INSERT) // 插入时自动填充
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime deliveryTime;

    // 收货时间
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime receiveTime;

    @TableField(fill = FieldFill.INSERT_UPDATE) // 插入和更新时自动填充
    private LocalDateTime updateTime;

    @TableLogic // 逻辑删除
    private Integer isDeleted;

    // 优惠券相关
    // private Long couponId;             // 优惠券ID
    private BigDecimal totalDiscountAmount;
    private String couponIds;
}
