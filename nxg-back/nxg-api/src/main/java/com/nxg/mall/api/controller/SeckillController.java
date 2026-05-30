package com.nxg.mall.api.controller;

import com.nxg.mall.common.Result;
import com.nxg.mall.common.vo.SeckillProductVO;
import com.nxg.mall.api.service.ISeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/seckill")
public class SeckillController {

    @Autowired
    private ISeckillService seckillService;

    @GetMapping("/list")
    public Result<List<SeckillProductVO>> getSeckillList() {
        return Result.success(seckillService.getActiveSeckillProducts());
    }
}
