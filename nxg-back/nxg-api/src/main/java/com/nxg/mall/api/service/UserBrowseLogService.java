package com.nxg.mall.api.service;

import java.time.LocalDateTime;

public interface UserBrowseLogService {
    /**
     * 记录进入行为
     * @return 返回该条日志记录的唯一标识ID
     */
    Long recordEnter(Long userId, Long productId, Long categoryId, String ip);

    /**
     * 记录离开行为（更新时长）
     */
    void recordLeave(Long logId, Integer duration);
}
