package com.nxg.mall.common.dto.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessageDTO {
    // 1. 订单主信息（来自你的 Order 实体）
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal payAmount;
    private Integer status;
    private LocalDateTime createTime;

    // 2. 订单明细快照（非常重要！用于前端显示单价、数量、类别）
    private List<OrderItemDetail> items;

    @Data
    public static class OrderItemDetail {
        private Long productId;
        private String productName;
        private String categoryName; // 建议在这里直接存入类别名称
        private BigDecimal unitPrice;
        private Integer quantity;
        private String productImage;
    }
}
