package com.nxg.mall.api.controller;

import com.nxg.mall.common.utils.SecurityUtils;
import com.nxg.mall.common.vo.UserCenterVO;
import com.nxg.mall.common.Result;
import com.nxg.mall.api.service.UserCenterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserCenterController {

    @Autowired
    private UserCenterService userCenterService;

    @GetMapping("/center")
    public Result<UserCenterVO> getUserCenterInfo() {
        // 从拦截器/上下文获取当前用户ID
        Long userId = SecurityUtils.getUserId();

        UserCenterVO vo = userCenterService.getUserCenterInfo(userId);
        return Result.success(vo);
    }
}
