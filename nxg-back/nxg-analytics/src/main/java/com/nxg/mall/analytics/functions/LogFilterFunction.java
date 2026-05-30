package com.nxg.mall.analytics.functions;

import com.nxg.mall.common.entity.log.UserBrowseLog;
import org.apache.flink.api.common.functions.FilterFunction;

public class LogFilterFunction implements FilterFunction<UserBrowseLog> {
    private static final long MIN_TIMESTAMP = 1704067200000L; // 2024-01-01
    private static final long MAX_TIMESTAMP = 2524608000000L; // 2050-01-01

    @Override
    public boolean filter(UserBrowseLog entity) {
        if (entity == null || entity.getUserId() == null ||
                entity.getEnterTime() == null || entity.getLeaveTime() == null) {
            return false;
        }

        long enter = entity.getEnterTime();
        long leave = entity.getLeaveTime();

        // 严格 13 位毫秒校验
        return enter >= MIN_TIMESTAMP && enter <= MAX_TIMESTAMP &&
                leave >= MIN_TIMESTAMP && leave <= MAX_TIMESTAMP &&
                leave >= enter;
    }
}
