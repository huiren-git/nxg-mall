package com.nxg.mall.api.controller;

import com.nxg.mall.api.vo.UserBaseInfoVO;
import com.nxg.mall.common.Result;
import com.nxg.mall.api.service.AuthService;
import com.nxg.mall.api.service.IUserService;
import com.nxg.mall.common.utils.SecurityUtils;
import com.nxg.mall.common.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private AuthService authService;

    @Autowired
    private IUserService userService;

    /**
     * [通用] 获取当前登录用户的基本信息
     * 自动从 JWT Token 中解析当前用户
     */
    @GetMapping("/profile")
    public Result<UserVO> getCurrentUserProfile() {
        // 假设你有一个工具类从 SecurityContext 获取当前 UserID
        Long currentUserId = SecurityUtils.getUserId();
        UserVO userVO = userService.getUserVOById(currentUserId);
        return Result.success(userVO);
    }

    // 在 nxg-api 模块的 UserController
    @GetMapping("/detail/base/{userId}")
    public Result<UserBaseInfoVO> getCustomerBaseInfo(@PathVariable Long userId) {
        // 内部实现由 UserMapper 和 UserStatusMapper 进行 left join 联查：
        // SELECT u.id as user_id, u.username, u.nickname, u.avatar, u.email, u.balance, u.create_time as register_time, s.last_login_time, s.last_login_ip
        // FROM t_user u LEFT JOIN t_user_status s ON u.id = s.user_id WHERE u.id = #{userId}
        UserBaseInfoVO baseInfo = userService.getCustomerBaseInfo(userId);
        return Result.success(baseInfo);
    }
}