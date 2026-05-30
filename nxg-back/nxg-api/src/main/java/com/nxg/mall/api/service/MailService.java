package com.nxg.mall.api.service;

import com.nxg.mall.common.dto.OrderEmailDTO;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Slf4j
public class MailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;


    @Async("taskExecutor")
    public void sendCodeEmail(String toEmail, String code, String sceneName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail + "(XX商城)");
            helper.setTo(toEmail);
            helper.setSubject("【XX商城】您的安全验证码：" + code);

            // 1. 构造模板上下文，向 HTML 传递变量
            Context context = new Context();
            context.setVariable("code", code);
            context.setVariable("sceneName", sceneName);

            // 2. 解析模板
            String htmlContent = templateEngine.process("email/captcha", context);

            // 3. 设置邮件正文为 HTML
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("HTML邮件已发送至：{}", toEmail);
        } catch (Exception e) {
            log.error("发送HTML邮件失败: {}", e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void sendResetLinkEmail(String toEmail, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail + "(XX商城)");
            helper.setTo(toEmail);
            helper.setSubject("【XX商城】重置您的登录密码");

            // 使用 Thymeleaf 模板发送
            Context context = new Context();
            context.setVariable("resetLink", resetLink);

            // 假设你在 resources/templates/ 下建了一个 email-reset.html
            String htmlContent = templateEngine.process("/email/reset_password", context);

            helper.setText(htmlContent, true);
            mailSender.send(message);

        } catch (Exception e) {
            log.error("发送重置密码邮件失败: {}", e.getMessage());
        }
    }

    /**
     * 异步发送发货通知邮件
     */
    @Async("taskExecutor") // 确保配置了线程池并开启了 @EnableAsync
    public void sendShippingEmail(OrderEmailDTO info) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(info.getEmail());
            helper.setSubject("【商城】您的订单已发货，请注意查收");

            // 渲染 Thymeleaf 模板
            Context context = new Context();
            context.setVariable("nickname", info.getNickname());
            context.setVariable("orderNo", info.getOrderNo());

            String htmlContent = templateEngine.process(info.getTemplate(), context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("发货邮件已发送至: {}", info.getEmail());
        } catch (Exception e) {
            // 邮件发送失败仅记录日志，不应影响业务流程
            log.error("发货邮件发送失败，订单号: {}, 原因: {}", info.getOrderNo(), e.getMessage());
        }
    }

    /**
     * 异步发送订单取消通知邮件
     */
    @Async("taskExecutor")
    public void sendCancelEmail(OrderEmailDTO info) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(info.getEmail());
            helper.setSubject("【商城】您的订单已被取消通知");

            Context context = new Context();
            context.setVariable("nickname", info.getNickname());
            context.setVariable("orderNo", info.getOrderNo());
            context.setVariable("reason", info.getReason()); // 模板中展示取消原因

            String htmlContent = templateEngine.process(info.getTemplate(), context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("订单取消邮件已发送至: {}", info.getEmail());
        } catch (Exception e) {
            log.error("订单取消邮件发送失败，订单号: {}, 原因: {}", info.getOrderNo(), e.getMessage());
        }
    }
}
