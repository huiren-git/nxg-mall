package com.nxg.mall.api.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.api.vo.PurchaseLogVO;
import com.nxg.mall.common.dto.OrderCancelDTO;
import com.nxg.mall.common.dto.OrderCreateDTO;
import com.nxg.mall.common.dto.OrderDeliveryDTO;
import com.nxg.mall.common.vo.OrderVO;
import com.nxg.mall.common.entity.Order;

import java.util.List;

public interface IOrderService extends IService<Order> {
    /**
     * 创建订单的核心业务
     */
    OrderVO createOrder(Long userId, OrderCreateDTO createDTO);

    /*
    *  列出订单列表
    * */
    List<OrderVO> getUserOrderList(Long userId, String status);

    /**
     * 取消订单
     * @param orderNo 订单号
     * @param userId 用户ID（用于越权校验）
     */
    void cancelOrder(String orderNo, Long userId);

    /**
     * 用户确认收货
     * @param orderNo 订单编号
     * @param userId 用户ID（用于越权校验）
     */
    void confirmReceipt(String orderNo, Long userId);

    /**用户结束订单
     * @param orderNo 订单编号
     * @param userId 用户ID（用于越权校验）
     * */
    void completeOrder(String orderNo, Long userId);

    /**
     * 用户申请退货
     * @param orderNo
     * @param userId
     */
    void applyReturn(String orderNo, Long userId);

    /*
    * 订单发货
     */
    void deliveryOrder(OrderDeliveryDTO param);

    /**
     * sales取消订单
     * @param param
     * @param operatorId
     */
    void cancelOrderBySales(OrderCancelDTO param, Long operatorId);

    /**
     * 销售人员同意退货
     * @param orderNo
     */
    void approveReturn(String orderNo);

    /**
     * 销售人员拒绝退货
     * @param orderNo
     * @param reason
     */
    void rejectReturn(String orderNo, String reason);

    /*
    * Sales或Admin 获得全部用户全部订单列表
    * */
    List<OrderVO> getAllCustomerOrderList(String status);

    /**
     * Sales后台中获取用户的购物日志
     * @param userId
     * @return
     */
    List<PurchaseLogVO> getPurchaseLogs(Long userId);
}
