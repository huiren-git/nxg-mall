package com.nxg.mall.api.controller;

import com.nxg.mall.common.Result;
import com.nxg.mall.common.dto.OrderCancelDTO;
import com.nxg.mall.common.dto.OrderCreateDTO;
import com.nxg.mall.common.dto.OrderDeliveryDTO;
import com.nxg.mall.common.vo.OrderVO;
import com.nxg.mall.api.service.IOrderService;
import com.nxg.mall.common.utils.SecurityUtils;
import com.nxg.mall.common.annotation.Log;
import com.nxg.mall.common.enums.ActionType;
import com.nxg.mall.common.enums.TargetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
public class OrderController {

    @Autowired
    private IOrderService orderService;

    // 创建订单
    @PostMapping("/customer/orders/create")
    public Result<OrderVO> createOrder(@RequestBody OrderCreateDTO createDTO) {
        // 获取用户id
        Long userId = SecurityUtils.getUserId();

        // 校验用户是否存在

        // 调用 Service 创建订单
        OrderVO orderVO = orderService.createOrder(userId, createDTO);

        return Result.success(orderVO);
    }

    // 获得用户的订单列表
    @GetMapping("/customer/orders/list")
    public Result<List<OrderVO>> getOrderList(@RequestParam(required = false) String status) {
        // 身份校验
        Long userId = SecurityUtils.getUserId();
        // 传递 String 类型的 status
        List<OrderVO> list = orderService.getUserOrderList(userId, status);
        return Result.success(list);
    }

    // 用户取消订单
    @PutMapping("/customer/orders/{orderNo}/cancel")
    public Result<Void> cancelOrder(@PathVariable String orderNo) {
        // 从安全上下文获取当前用户ID
        Long userId = SecurityUtils.getUserId();
        orderService.cancelOrder(orderNo, userId);
        return Result.success();
    }

    // 用户确认收货
    @PutMapping("/customer/orders/{orderNo}/confirm")
    public Result<String> confirmReceipt(@PathVariable String orderNo) {
        // 获取用户id
        Long userId = SecurityUtils.getUserId();

        orderService.confirmReceipt(orderNo, userId);
        return Result.success("确认收货成功");
    }

    // 用户结束订单
    @PutMapping("/customer/orders/{orderNo}/complete")
    public Result<String> completeOrder(@PathVariable String orderNo) {
        // 1. 从安全上下文获取当前登录用户 ID (实际开发中通常通过 JWT 解析)
        Long userId = SecurityUtils.getUserId();

        // 2. 调用业务层逻辑
        orderService.completeOrder(orderNo, userId);

        return Result.success("订单已确认完成");
    }

    // 用户申请售后
    @PostMapping("customer/orders/{orderNo}/return")
    public Result<String> applyReturn(@PathVariable String orderNo) {
        Long userId = SecurityUtils.getUserId();
        orderService.applyReturn(orderNo, userId);
        return Result.success("申请成功");
    }

    // 销售人员发货
    @Log(title = "订单发货", actionType = ActionType.DELIVER, targetType = TargetType.ORDER)
    @PostMapping("/sales/orders/delivery")
    @PreAuthorize("hasRole('SALES')")
    public Result<String> deliveryOrder(@RequestBody OrderDeliveryDTO param) {
        orderService.deliveryOrder(param);
        return Result.success("发货成功");
    }

    // 销售人员取消订单
    @Log(title = "销售取消订单", actionType = ActionType.CANCEL_ORDER, targetType = TargetType.ORDER)
    @PreAuthorize("hasRole('SALES')")
    @PutMapping("/sales/orders/cancel")
    public Result<Void> salesCancelOrder(@RequestBody @Validated OrderCancelDTO param) {
        // 获取当前登录的后台人员ID（可选，用于审计）
        Long operatorId = SecurityUtils.getUserId();
        orderService.cancelOrderBySales(param, operatorId);
        return Result.success();
    }

    // 销售同意售后
    @Log(title = "同意退货", actionType = ActionType.REFUND_APPROVE, targetType = TargetType.ORDER)
    @PreAuthorize("hasRole('SALES')")
    @PostMapping("/sales/orders/{orderNo}/approve-return")
    public Result<String> approveReturn(@PathVariable String orderNo) {
        orderService.approveReturn(orderNo);
        return Result.success("已同意退货并完成退款");
    }

    // 拒绝退货申请
    @Log(title = "拒绝退货", actionType = ActionType.REFUND_REJECT, targetType = TargetType.ORDER)
    @PreAuthorize("hasRole('SALES')")
    @PostMapping("/sales/orders/{orderNo}/reject-return")
    public Result<String> rejectReturn(@PathVariable String orderNo, @RequestParam String reason) {
        orderService.rejectReturn(orderNo, reason);
        return Result.success("已拒绝退货申请");
    }

    // admin或者销售人员查看订单列表
    @GetMapping("/manage/orders/list")
    public Result<List<OrderVO>> getAllOrderList(@RequestParam(required = false) String status) {
        // 销售端不需要传入 userId，直接查询全表或按状态过滤
        System.out.println("当前用户权限: " + SecurityContextHolder.getContext().getAuthentication().getAuthorities());
        List<OrderVO> list = orderService.getAllCustomerOrderList(status);
        return Result.success(list);
    }
}
