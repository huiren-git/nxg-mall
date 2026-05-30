package com.nxg.mall.common.dto.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrafficLogMessageDTO {
    private String type;          // 行为类型：SEARCH (搜索), LIST (分类浏览)
    private Long userId;        // 当前用户ID（如果有）
    private Long categoryId;    // 浏览的分类ID
    private String keyword;     // 搜索关键词
    private LocalDateTime timestamp; // 发生时间
    private String clientIp;    // 客户端IP（可选，用于地域分析）
}
