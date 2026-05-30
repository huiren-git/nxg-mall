package com.nxg.mall.analytics.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nxg.mall.analytics.dto.UserPersonaDTO;
import com.nxg.mall.analytics.entity.CustomerStat;
import com.nxg.mall.analytics.model.dto.CustomerQueryDTO;
import com.nxg.mall.analytics.service.ICustomerStatService;
import com.nxg.mall.analytics.vo.TrendDataVO;
import com.nxg.mall.analytics.vo.UserPersonaVO;
import com.nxg.mall.analytics.vo.UserSummaryVO;
import com.nxg.mall.common.Result;
import com.nxg.mall.common.vo.CustomerDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/customer")
public class CustomerAnalyticsController {

    @Autowired
    private ICustomerStatService customerStatService;

    @GetMapping("/list")
    public Result<IPage<CustomerStat>> list(CustomerQueryDTO queryDTO) {
        // 调用 Service 获取分页对象
        IPage<CustomerStat> pageData = customerStatService.getCustomerPage(queryDTO);
        // 使用 Result.success() 进行封装
        return Result.success(pageData);
    }

    /**
     * 获取指定用户的画像数据
     * 符合 RESTful: GET /api/analytics/stat/persona/{userId}
     */
    @GetMapping("/persona/{userId}")
    public Result<UserPersonaVO> getCustomerPersona(@PathVariable Long userId) {
        UserPersonaVO persona = customerStatService.getPersonaByUserId(userId);
        if (persona == null) {
            return Result.error("暂无该用户的画像数据");
        }
        return Result.success(persona);
    }

    @GetMapping("/summary")
    public Result<UserSummaryVO> getSummary(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(customerStatService.getSummary(startDate, endDate));
    }

    @GetMapping("/trend/{type}")
    public Result<TrendDataVO> getTrend(
            @PathVariable String type,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(customerStatService.getTrend(type, startDate, endDate));
    }
}
