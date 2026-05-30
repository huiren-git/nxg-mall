package com.nxg.mall.analytics.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户画像明细实体类
 * 对应数据库表: t_user_portrait
 */
@Data
@TableName("t_user_portrait")
public class UserInsight {

    /**
     * 用户ID
     */
    @TableId
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 总浏览次数
     */
    private Integer totalBrowseCount;

    /**
     * 最偏好的商品品类
     */
    private String favoriteCategory;

    /**
     * 平均停留时间 (秒)
     */
    private Integer avgStayTime;

    /**
     * 累计消费总额
     */
    private BigDecimal totalSpent;

    /**
     * 累计订单数
     */
    private Integer orderCount;

    /**
     * 最近一次交易时间
     */
    private LocalDateTime lastTradeTime;

    /**
     * 最近一次登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最近一次登录IP
     */
    private String lastLoginIp;

    /**
     * 活跃度等级 (如: 高活跃、中活跃、低活跃)
     */
    private String activeLevel;

    /**
     * 所属省份
     */
    private String province;

    /**
     * 所属城市
     */
    private String city;

    /**
     * 记录更新时间
     * database 字段含有 DEFAULT_GENERATED on update CURRENT_TIMESTAMP
     */
    private LocalDateTime updateTime;
}