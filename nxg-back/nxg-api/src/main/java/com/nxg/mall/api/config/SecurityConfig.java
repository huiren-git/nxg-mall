package com.nxg.mall.api.config;

import com.nxg.mall.common.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Collections;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. 基础公共资源
                        .requestMatchers("/auth/**", "/error", "/uploads/**").permitAll()

                        // 2. 浏览/查询类接口 (允许游客访问，但如果有Token也能记录UID)
                        .requestMatchers("/products/**", "/category/**", "/seckill/**", "/coupons/**").permitAll()

                        // 3. 关键：日志采集接口 (显式指定权限，注意权限前缀)
                        // 如果你的JWT过滤器里没加 ROLE_ 前缀，请改用 .hasAnyAuthority("CUSTOMER", "ADMIN", "SALES")
                        .requestMatchers("/collect/**").hasAnyRole("CUSTOMER", "ADMIN", "SALES")

                        // 4. 业务逻辑接口
                        .requestMatchers("/manage/**").hasAnyRole("ADMIN", "SALES")
                        .requestMatchers("/customer/**").hasRole("CUSTOMER")
                        .requestMatchers("/sales/**").hasRole("SALES")
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 5. 其他放行
                        .requestMatchers("/user/center", "/cart/**", "/address/**", "/pay/**").permitAll()

                        // 6. 兜底
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        // 关键：改用 allowedOriginPatterns，允许所有域名和公网IP跨域
        config.setAllowedOriginPatterns(Collections.singletonList("*"));
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addExposedHeader("Authorization");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}