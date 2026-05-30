package com.nxg.mall.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailUtil {

    /**
     * 异步发送邮件（暂不实际发送，仅记录日志）
     * @param to 收件人邮箱
     * @param code 验证码
     */
    public void sendCodeAsync(String to, String code) {
        // TODO: 实际发送邮件逻辑
        log.info("【TODO】发送验证码邮件至：{}，验证码：{}", to, code);
        // 后续可替换为真实邮件发送
    }
}