package com.nxg.mall.common.dto; // 请根据你的项目实际包路径修改

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单邮件通知数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEmailDTO {

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 用户昵称 (用于邮件开头的称呼)
     */
    private String nickname;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 邮件模板路径 (对应 resources/templates/ 下的路径，如 "email/shipping")
     */
    private String template;

    /**
     * 订单取消原因 (针对取消订单邮件模板)
     */
    private String reason;

    /**
     * 跳转链接 (如订单详情页、评价页或商城首页的 URL)
     */
    private String url;

    /**
     * 确认收货链接 (针对提醒收货模板)
     */
    private String confirmUrl;

    /**
     * 评价页面链接 (针对提醒评价模板)
     */
    private String commentUrl;
}