package com.nxg.mall.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.regex.Pattern;

@Slf4j
@Component
public class AntiCrawlerInterceptor implements HandlerInterceptor {

    // 编译好常见非浏览器脚本工具和爬虫的正则表达式（忽略大小写）
    private static final Pattern BOT_PATTERN = Pattern.compile(
            "python|scrapy|curl|wget|httpclient|postman|apachebench|java|perl|go-http-client",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userAgent = request.getHeader("User-Agent");

        // 1. 策略一：绝对禁止没有 User-Agent 的请求
        if (userAgent == null || userAgent.trim().isEmpty()) {
            log.warn("【防爬拦截】拒绝无 User-Agent 的恶意请求，来源IP: {}，请求路径: {}",
                    request.getRemoteAddr(), request.getRequestURI());
            sendErrorResponse(response);
            return false;
        }

        // 2. 策略二：识别非浏览器的自动化脚本工具特征
        if (BOT_PATTERN.matcher(userAgent).find()) {
            log.warn("【防爬拦截】拦截到非浏览器自动化工具！UA: [{}], 来源IP: {}, 请求路径: {}",
                    userAgent, request.getRemoteAddr(), request.getRequestURI());
            sendErrorResponse(response);
            return false;
        }

        // 验证通过，放行
        return true;
    }

    /**
     * 统一返回 403 错误响应（保持与你项目一致的 JSON 格式）
     */
    private void sendErrorResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
        response.setContentType("application/json;charset=UTF-8");
        // 匹配你的标准响应结构 {"code":403, "message":"...", "data":null}
        response.getWriter().write("{\"code\":403,\"message\":\"请求不合法，请使用标准浏览器访问\",\"data\":null}");
    }
}
