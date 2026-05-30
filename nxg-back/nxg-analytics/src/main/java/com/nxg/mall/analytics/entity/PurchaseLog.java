package com.nxg.mall.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_purchase_log") // 指定 MyBatis-Plus 对应的表名
public class PurchaseLog {

    @TableId(type = IdType.AUTO) // 主键自增
    private Long id;

    private String orderNo;

    private Long userId;

    private String productName;

    private String category;

    private BigDecimal unitPrice;

    private Integer quantity;

    /**
     * 下单时间：由业务系统传入
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime purchaseTime;

    /**
     * 分析入库时间：数据库自动生成，对应表中的 CURRENT_TIMESTAMP
     * 在 Java 端通常不需要手动设置
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createAt;
}
