package com.nxg.mall.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.common.entity.OrderItem;
import com.nxg.mall.api.mapper.OrderItemMapper;
import com.nxg.mall.api.service.IOrderItemService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements IOrderItemService {
    @Override
    public List<OrderItem> listByOrderNo(String orderNo) {
        if (StringUtils.isBlank(orderNo)) {
            return Collections.emptyList();
        }

        // 使用 LambdaQuery 避免硬编码数据库字段名，提高维护性
        return this.list(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderNo, orderNo)
                .orderByAsc(OrderItem::getId)); // 建议按 ID 或创建时间排序，保证展示顺序一致
    }
}
