package com.nxg.mall.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nxg.mall.common.Result;
import com.nxg.mall.common.annotation.Log;
import com.nxg.mall.common.dto.CustomerQueryDTO;
import com.nxg.mall.common.dto.SalesAddDTO;
import com.nxg.mall.common.dto.SalesUpdateDTO;
import com.nxg.mall.api.service.IUserService;
import com.nxg.mall.common.enums.ActionType;
import com.nxg.mall.common.enums.TargetType;
import com.nxg.mall.common.vo.CustomerVO;
import com.nxg.mall.common.vo.ManageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    @Autowired
    private IUserService userService; // 假设销售数据也在 user 表中，通过 role 区分

    @Log(title = "添加销售人员", actionType = ActionType.ADD_SALES, targetType = TargetType.SALES, isSaveRequestData = false)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Result<String> addSales(@RequestBody SalesAddDTO salesAddDTO) {
        // 调用 service 层逻辑
        userService.saveSales(salesAddDTO);
        return Result.success("添加销售人员成功");
    }


    @GetMapping
    public Result<IPage<ManageVO>> getAdminList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer role) {

        IPage<ManageVO> result = userService.getManagePage(page, size, name, role);
        return Result.success(result);
    }

    /**
     * 重置顾客密码
     * RESTful: PUT /api/admin/user/{id}/password/reset
     */
    @Log(title = "重置顾客密码", actionType = ActionType.RESET_PWD, targetType = TargetType.USER, isSaveRequestData = false)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/customer-password/reset")
    public Result<String> resetCustomerPassword(@PathVariable Long id) {
        boolean success = userService.resetCustomerPassword(id);
        if (success) {
            return Result.success("密码重置成功");
        }
        return Result.error("重置失败，用户不存在或非顾客角色");
    }

    // 更新销售信息
    @Log(
            title = "更新销售人员信息",
            actionType = ActionType.UPDATE_SALES, // 或者使用你定义的 RESET_PWD（如果是ActionType枚举的话）
            targetType = TargetType.SALES,
            isSaveRequestData = false // 密码重置操作，建议关闭请求参数保存，防止敏感信息进入日志
    )
    @PutMapping("/{id}")
    public Result<String> updateSales(@PathVariable Long id, @RequestBody SalesUpdateDTO dto) {
        userService.updateSales(id, dto);
        return Result.success("更新成功");
    }

    // 重置密码
    @Log(
            title = "重置SALES密码",
            actionType = ActionType.RESET_PWD, // 或者使用你定义的 RESET_PWD（如果是ActionType枚举的话）
            targetType = TargetType.USER,
            isSaveRequestData = false // 密码重置操作，建议关闭请求参数保存，防止敏感信息进入日志
    )
    @PutMapping("/{id}/sales-password/reset")
    public Result<String> resetSalesPassword(@PathVariable Long id) {
        String defaultPassword = "123456"; // 也可以让管理员输入，这里演示重置为默认
        userService.resetPassword(id, defaultPassword);
        return Result.success("密码已重置为：" + defaultPassword);
    }

    // 删除指定销售人员
    @Log(
            title = "删除销售人员",
            actionType = ActionType.DELETE_SALES, // 或者使用你定义的 RESET_PWD（如果是ActionType枚举的话）
            targetType = TargetType.SALES,
            isSaveRequestData = false // 密码重置操作，建议关闭请求参数保存，防止敏感信息进入日志
    )
    @DeleteMapping("/{id}")
    public Result<String> deleteSales(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success("删除成功");
    }


}
