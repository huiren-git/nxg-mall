package com.nxg.mall.common.interceptor;

import com.nxg.mall.common.constant.RedisConstants;
import com.nxg.mall.common.exception.BusinessException; // 换成你项目实际的异常类
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ProductRateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取客户端真实 IP
        String clientIp = request.getHeader("X-Forwarded-For");
        log.info(">>>> 拦截器触发，IP: {}, Path: {}", clientIp, request.getRequestURI());

        // 2. 检查该 IP 是否处于 1小时 封禁状态
        String lockKey = RedisConstants.PRODUCT_LIMIT_LOCK_PREFIX + clientIp;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            log.warn("【限流拒绝】IP: {} 处于封禁状态，拒绝访问商品接口", clientIp);
            sendErrorResponse(response, "操作过于频繁，账号安全保护中，请1小时后再试");
            return false;
        }

        // 3. 构建秒级计数 Key (格式如 -> limit:count:product:127.0.0.1:1716900000)
        long currentSecond = System.currentTimeMillis() / 1000;
        String countKey = RedisConstants.PRODUCT_LIMIT_COUNT_PREFIX + clientIp + ":" + currentSecond;

        // 4. 原子自增
        Long currentCount = redisTemplate.opsForValue().increment(countKey, 1);

        // 如果是当前秒的第一次访问，设置 2 秒过期时间（多给1秒防止临界点突变，自动释放内存）
        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(countKey, 2, TimeUnit.SECONDS);
        }

        // 5. 判断是否违规（每秒超过 5 次）
        if (currentCount != null && currentCount > RedisConstants.MAX_PRODUCT_REQUEST_PER_SECOND) {
            // 触发惩罚机制：锁定 1 小时
            redisTemplate.opsForValue().set(lockKey, "LOCKED", RedisConstants.PRODUCT_LOCK_EXPIRE_HOURS, TimeUnit.HOURS);
            log.error("【接口防刷】检测到恶意刷接口行为！IP: {}，当前秒请求数: {}，已封禁 1 小时！", clientIp, currentCount);

            sendErrorResponse(response, "请求过于频繁，系统检测到异常访问，已被封禁1小时");
            return false;
        }

        return true;
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
        response.setContentType("application/json;charset=UTF-8");
        // 严格匹配你项目的 {"code":403, "message":"..."} 格式
        response.getWriter().write(String.format("{\"code\":403,\"message\":\"%s\",\"data\":null}", message));
    }
}
