package com.nxg.mall.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserBalanceLog {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 业务流水号 (例如: REC20260513... 或 PAY20260513...)
     */
    private String orderNo;

    /**
     * 变更金额
     */
    private BigDecimal amount;

    /**
     * 交易类型: 1-充值, 2-消费, 3-退款
     */
    private Integer type;

    /**
     * 交易状态: 0-进行中, 1-成功, 2-失败
     */
    private Integer status;

    /**
     * 创建时间
     * fill = FieldFill.INSERT 表示在插入时自动填充（需配置 MybatisPlusMetaObjectHandler）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 备注 (可选，用于记录如 "支付宝充值"、"购买商品支出" 等信息)
     */
    private String remark;
}
