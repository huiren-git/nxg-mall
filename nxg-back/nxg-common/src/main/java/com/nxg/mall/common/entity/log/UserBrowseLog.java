package com.nxg.mall.common.entity.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 内部日志实体：用于 Kafka 传输的完整日志数据载体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBrowseLog implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;       // 从 SecurityContext 获取
    private Long productId;    // 从前端传参获取
    private Long categoryId;   // 从前端传参获取
    private Long enterTime; // 进入时间
    private Long leaveTime; // 离开时间
    private Integer duration;  // 停留时长（秒）
    private String clientIp;   // 服务端获取的 IP
    private String terminal;   // 前端获取的终端类型 (PC/Mobile)
    private String sourceType;
}
