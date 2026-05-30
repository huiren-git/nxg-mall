package com.nxg.mall.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.common.entity.OrderItem;

import java.util.List;

public interface IOrderItemService extends IService<OrderItem> {
    /**
     * 根据订单号查询所有关联的订单明细
     * @param orderNo 订单编号
     * @return 订单明细列表
     */
    List<OrderItem> listByOrderNo(String orderNo);
}
