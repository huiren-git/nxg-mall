package com.nxg.mall.api.service;

import com.nxg.mall.common.dto.*;
import com.nxg.mall.common.vo.LoginResponseVO;
import com.nxg.mall.common.vo.UserCenterInfoVO;

public interface AuthService {

    void sendCode(SendCodeRequest request);
    void register(RegisterRequest request, String clientIp);
    LoginResponseVO login(LoginRequest request, String clientIp);

    // 根据token获取用户信息
    UserCenterInfoVO getUserProfile(String token);

    void updatePassword(Long userId, PasswordUpdateDTO dto);
    void updateEmail(Long userId, EmailUpdateDTO dto);

    void forgotPassword(ForgotPasswordDTO dto);

    void resetPassword(ResetPasswordDTO dto);
}