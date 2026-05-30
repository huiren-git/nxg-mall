package com.nxg.mall.analytics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.analytics.entity.AnomalyLog;
import com.nxg.mall.analytics.mapper.AnomalyLogMapper;
import com.nxg.mall.analytics.service.IAnomalyLogService;
import com.nxg.mall.analytics.vo.AnomalyDashboardVO;
import com.nxg.mall.analytics.vo.AnomalyLogVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.nxg.mall.common.constant.RedisConstants.*;

@Slf4j
@Service
public class AnomalyLogServiceImpl extends ServiceImpl<AnomalyLogMapper, AnomalyLog> implements IAnomalyLogService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public AnomalyDashboardVO getDashboardMetrics() {
        AnomalyDashboardVO vo = new AnomalyDashboardVO();

        // 1. 读取 Redis 实时统计基础指标（带安全解析与兜底防御）
        vo.setTotalToday(getRedisIntOrDefault(ANOMALY_TOTAL_TODAY, 0));
        vo.setInterceptedCount(getRedisIntOrDefault(ANOMALY_INTERCEPTED_COUNT, 0));
        vo.setCurrentQps(getRedisIntOrDefault(SYSTEM_CURRENT_QPS, 0));
        vo.setAvgResponseTime(getRedisIntOrDefault(SYSTEM_AVG_RT, 0));

        // 健康分：优先读 Redis，读不到则根据今日异常数动态计算扣分（更具真实感）
        Integer healthScore = getRedisIntOrDefault(SYSTEM_HEALTH_SCORE, null);
        if (healthScore == null) {
            healthScore = Math.max(60, 100 - (vo.getTotalToday() / 5)); // 每5个异常扣1分，最低60分
        }
        vo.setHealthScore(healthScore);


        // 2. 动态计算 Kafka 滑动窗口时间轴（向前推 7 个 10 分钟窗口）
        java.util.List<String> xAxisData = new java.util.ArrayList<>();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        // 将分钟对齐到 10 分钟向下取整（例如 20:16 转换为 20:10）
        int alignedMinute = (now.getMinute() / 10) * 10;
        java.time.LocalDateTime currentWindow = now.withMinute(alignedMinute).withSecond(0).withNano(0);

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDateTime windowTime = currentWindow.minusMinutes(i * 10L);
            xAxisData.add(windowTime.format(formatter));
        }
        vo.setXAxisData(xAxisData);


        // 3. 批量从 Redis Hash 中提取真实的 Actual（实际值）和 Baseline（基线值）
        // 使用 multiGet 减少网络 IO 交互次数，高并发下性能极佳
        java.util.List<Object> actualValues = redisTemplate.opsForHash().multiGet(ANOMALY_CHART_ACTUAL_HASH, new java.util.ArrayList<>(xAxisData));
        java.util.List<Object> baselineValues = redisTemplate.opsForHash().multiGet(ANOMALY_CHART_BASELINE_HASH, new java.util.ArrayList<>(xAxisData));

        // 转换并清洗 Actual 数据
        java.util.List<Integer> actualData = actualValues.stream()
                .map(val -> val != null ? Integer.parseInt(val.toString()) : 0) // 没数据的窗口兜底为 0
                .collect(java.util.stream.Collectors.toList());
        vo.setActualData(actualData);

        // 转换并清洗 Baseline 数据
        java.util.List<Integer> baselineData = baselineValues.stream()
                .map(val -> val != null ? Integer.parseInt(val.toString()) : 1000) // 基线没数据默认给个安全水位
                .collect(java.util.stream.Collectors.toList());
        vo.setBaselineData(baselineData);

        return vo;
    }

    /**
     * 辅助方法：安全获取 Redis 中的 Integer 值
     */
    private Integer getRedisIntOrDefault(String key, Integer defaultValue) {
        try {
            String val = redisTemplate.opsForValue().get(key);
            if (org.springframework.util.StringUtils.hasText(val)) {
                return Integer.parseInt(val);
            }
        } catch (Exception e) {
            log.error("获取大盘指标 Redis Key 异常: {}", key, e);
        }
        return defaultValue;
    }


    @Override
    public List<AnomalyLogVO> getPendingLogs() {
        // 从 MySQL 查询状态为 0 (待处理) 的记录，按时间倒序
        LambdaQueryWrapper<AnomalyLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnomalyLog::getStatus, 0)
                .orderByDesc(AnomalyLog::getCreateTime);

        List<AnomalyLog> logs = this.list(wrapper);

        // 转化为前端需要的 VO
        return logs.stream().map(log -> {
            AnomalyLogVO vo = new AnomalyLogVO();
            vo.setId(log.getLogNo()); // 前端 key 使用 logNo
            // 格式化时间，去掉年月日，只保留时分秒匹配前端UI
            vo.setTime(DateUtil.format(log.getCreateTime(), "HH:mm:ss"));
            vo.setType(log.getAnomalyType());
            vo.setTarget(log.getTargetObj());
            vo.setLevel(log.getRiskLevel());
            vo.setDetail(log.getDetailDesc());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean handleLog(String logNo, Integer actionType) {
        // actionType: 1-拦截, 2-忽略
        LambdaQueryWrapper<AnomalyLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnomalyLog::getLogNo, logNo);

        AnomalyLog updateEntity = new AnomalyLog();
        updateEntity.setStatus(actionType);

        return this.update(updateEntity, wrapper);
    }

    @Override
    public boolean handleAllPending() {
        // 将所有状态为 0 的更新为 2 (忽略/已读)
        LambdaQueryWrapper<AnomalyLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnomalyLog::getStatus, 0);

        AnomalyLog updateEntity = new AnomalyLog();
        updateEntity.setStatus(2);

        return this.update(updateEntity, wrapper);
    }
}