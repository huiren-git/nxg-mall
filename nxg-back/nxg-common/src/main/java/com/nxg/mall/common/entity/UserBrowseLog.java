package com.nxg.mall.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户浏览日志实体类
 * 对应表名：t_user_browse_log
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBrowseLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID (YES, 可为空，适配游客访问)
     */
    private Long userId;

    /**
     * 商品ID (NO, 不可为空)
     */
    private Long productId;

    /**
     * 商品类别ID (NO, 不可为空)
     */
    private Long categoryId;

    /**
     * 进入页面的时间 (NO, 不可为空)
     */
    private LocalDateTime enterTime;

    /**
     * 离开页面的时间 (NO, 不可为空)
     */
    private LocalDateTime leaveTime;

    /**
     * 停留时长，单位秒 (NO, 不可为空)
     */
    private Integer duration;

    /**
     * 用户IP地址 (YES, 可为空)
     */
    private String clientIp;

    /**
     * 终端类型，如：PC, APP, H5 (NO, 不可为空)
     */
    private String terminal;

}