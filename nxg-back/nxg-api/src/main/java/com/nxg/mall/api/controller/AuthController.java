package com.nxg.mall.api.controller;

import com.nxg.mall.common.Result;
import com.nxg.mall.common.dto.*;
import com.nxg.mall.common.utils.SecurityUtils;
import com.nxg.mall.common.vo.LoginResponseVO;
import com.nxg.mall.api.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        authService.sendCode(request);
        return Result.success();
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        authService.register(request, clientIp);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<LoginResponseVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        // 获取客户端 IP 用于记录登录 IP（在 Service 中需要传入）
        String clientIp = getClientIp(httpRequest);
        LoginResponseVO response = authService.login(request, clientIp);
        return Result.success(response);
    }

    /**
     * 修改登录密码
     */
    @PutMapping("/password")
    public Result<Void> updatePassword(@RequestBody @Validated PasswordUpdateDTO dto) {
        Long userId = SecurityUtils.getUserId();
        authService.updatePassword(userId, dto);
        return Result.success();
    }

    /**
     * 发送重置密码邮件（忘记密码）
     */
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@RequestBody @Validated ForgotPasswordDTO dto) {
        authService.forgotPassword(dto);
        return Result.success();
    }

    /**
     * 通过 Token 重置密码
     */
    @PutMapping("/password/reset")
    public Result<Void> resetPassword(@RequestBody @Validated ResetPasswordDTO dto) {
        authService.resetPassword(dto);
        return Result.success();
    }

    /**
     * 修改绑定邮箱
     */
    @PutMapping("/email")
    public Result<Void> updateEmail(@RequestBody @Validated EmailUpdateDTO dto) {
        Long userId = SecurityUtils.getUserId();
        authService.updateEmail(userId, dto);
        return Result.success();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}