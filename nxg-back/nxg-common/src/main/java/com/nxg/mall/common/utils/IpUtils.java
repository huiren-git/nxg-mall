package com.nxg.mall.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IP 操作工具类
 */
public class IpUtils {

    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IPV4 = "127.0.0.1";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";

    /**
     * 获取当前请求的客户端真实 IP 地址
     * (无参方法，依赖 Spring 的 RequestContextHolder，可在 Service 层直接调用)
     *
     * @return 真实 IP 地址
     */
    public static String getIpAddr() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return UNKNOWN;
        }
        return getIpAddr(attributes.getRequest());
    }

    /**
     * 获取客户端的真实 IP 地址
     *
     * @param request HttpServletRequest
     * @return 真实 IP 地址
     */
    public static String getIpAddr(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        // 走到这里，说明前面都没有获取到，只能使用 getRemoteAddr()
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
            // 如果获取到的是本机环回地址，尝试获取本机真正的网卡 IP
            if (LOCALHOST_IPV4.equals(ip) || LOCALHOST_IPV6.equals(ip)) {
                try {
                    InetAddress inet = InetAddress.getLocalHost();
                    ip = inet.getHostAddress();
                } catch (UnknownHostException e) {
                    // 忽略异常，保留 127.0.0.1
                }
            }
        }

        // 对于通过多个代理的情况，第一个 IP 为客户端真实 IP，多个 IP 会按照 ',' 分割
        // 例： "192.168.1.10, 10.0.0.1, 172.16.0.1"
        if (StringUtils.hasText(ip) && ip.length() > 15) {
            if (ip.indexOf(",") > 0) {
                ip = ip.substring(0, ip.indexOf(","));
            }
        }

        return ip;
    }

    /**
     * 判断是否为内网 IP
     * 常用于后台接口的安全校验
     *
     * @param ip IP地址
     * @return 是否为内网 IP
     */
    public static boolean isInternalIp(String ip) {
        if (!StringUtils.hasText(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            return false;
        }
        if (LOCALHOST_IPV4.equals(ip) || LOCALHOST_IPV6.equals(ip)) {
            return true;
        }
        // 匹配常见的私有网段
        return ip.startsWith("10.") ||
                ip.startsWith("192.168.") ||
                ip.matches("^172\\.(1[6-9]|2\\d|3[0-1])\\..*");
    }
}
