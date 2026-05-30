package com.nxg.mall.common.entity.log;

import lombok.Data;

/**
 * 这个类仅仅是 Kafka 消息的载体
 * 它的字段必须和你在 nxg-api 发送 Kafka 消息时的 JSON 键名完全一致
 */
@Data
public class LogMessage {
    // 基础三要素（所有日志共有）
    private Long userId;
    private String clientIp;
    private String actionType; // BROWSE, SEARCH, ORDER

    // 扩展信息（根据 actionType 不同，选填）
    private Long categoryId;   // 浏览或下单时填
    private String keyword;    // 搜索时填
    private Long productId;    // 浏览或下单时填
}
