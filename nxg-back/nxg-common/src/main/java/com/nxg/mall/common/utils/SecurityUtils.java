package com.nxg.mall.common.utils;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;

/**
 * Spring Security 环境下的用户工具类
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户 ID
     * 逻辑：从 SecurityContext 中获取 Authentication，再取出 Principal
     */
    public static Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        // 如果你在拦截器里存的是 Long 类型
        if (principal instanceof Long) {
            return (Long) principal;
        }

        // 如果存的是 String 类型则进行转换
        if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    /**
     * 获取用户名
     */
    public static String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "unknown";
        }
        return authentication.getName(); // 返回登录账号
    }

    /**
     * 获取用户角色 (返回第一个匹配的业务角色)
     */
    public static String getUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "GUEST";
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            // 过滤 Spring Security 常用的 ROLE_ 前缀
            if (role.startsWith("ROLE_")) {
                role = role.replace("ROLE_", "");
            }
            // 返回匹配你业务需要的角色名称
            if ("ADMIN".equalsIgnoreCase(role) || "SALES".equalsIgnoreCase(role)) {
                return role.toUpperCase();
            }
        }
        return "UNKNOWN";
    }

    /**
     * 判断当前是否有用户登录
     */
    public static boolean isLogin() {
        return getUserId() != null;
    }
}