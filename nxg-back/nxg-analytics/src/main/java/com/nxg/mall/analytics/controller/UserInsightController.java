package com.nxg.mall.analytics.controller;

import com.nxg.mall.analytics.service.IUserInsightService;
import com.nxg.mall.analytics.vo.UserInsightVO;
import com.nxg.mall.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/user")
public class UserInsightController {

    @Autowired
    private IUserInsightService userInsightService;

    @GetMapping("/insight")
    public Result<UserInsightVO> getInsight(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        UserInsightVO data = userInsightService.getUserInsightData(date);
        return Result.success(data);
    }
}