package com.nxg.mall.common.filter;

import com.nxg.mall.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils; // 引入 Spring 自带的工具类
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 如果是预检请求（CORS），直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader("Authorization");

        // 容错：如果前端传了 "Bearer null" 或 "Bearer undefined"
        if (token != null && (token.equals("Bearer null") || token.equals("Bearer undefined"))) {
            token = null;
        }

        // 2. 检查 Token 格式
        if (token != null && token.startsWith("Bearer ")) {
            try {
                String realToken = token.substring(7);

                // 解析 Token 拿到载荷
                Claims claims = jwtUtil.parseToken(realToken);

                // 🛡️ 确保 Subject 不为空再解析（若你的 Token 存的是用户名，请去修改 JwtUtil 存入 userId）
                String subject = claims.getSubject();

                if (StringUtils.hasText(subject) && SecurityContextHolder.getContext().getAuthentication() == null) {
                    Long userId = Long.parseLong(subject);

                    // 🛡️ 防御修复：提取 role 并安全地补全 "ROLE_" 前缀
                    String role = claims.get("role", String.class);
                    List<SimpleGrantedAuthority> authorities;

                    if (StringUtils.hasText(role)) {
                        // 如果不带 ROLE_ 前缀就帮它加上，确保符合 Spring Security 规范
                        String finalRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                        authorities = Collections.singletonList(new SimpleGrantedAuthority(finalRole));
                    } else {
                        // 如果确实没有角色，给一个兜底的匿名角色，防止创建 SimpleGrantedAuthority 报错
                        authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_GUEST"));
                    }

                    // 3. 构建完整的认证 Token 并注入上下文
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // 💡 本地调试神器：如果解析失败，控制台会清晰打印出原因（如 Token 过期、格式错误、或传了字母账号名）
                logger.error("!!!!!!!!!! JWT Auth Fail: " + e.getMessage(), e);
            }
        }

        // 4. 无论认证成功与否，都要放行给后面的过滤器（配合 permitAll 决定最终生死）
        filterChain.doFilter(request, response);
    }
}