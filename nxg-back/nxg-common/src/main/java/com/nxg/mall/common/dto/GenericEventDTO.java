package com.nxg.mall.common.dto;

import com.nxg.mall.common.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenericEventDTO<T> implements Serializable {
    // 1. 核心元数据 (所有事件必备)
    private String eventId;      // 唯一事件ID (UUID)
    private EventType type;      // 事件类型
    private Long timestamp;      // 触发时间戳
    private Long userId;         // 关联用户ID (匿名用户可为0或null)

    // 2. 环境元数据
    private String ip;           // 触发IP (用于ip2region解析)
    private String deviceId;     // 设备唯一标识
    private String platform;     // web, android, ios

    // 3. 业务载荷 (具体的业务数据)
    // T 可以是 Map<String, Object>，也可以是具体的业务实体
    private T data;
}
