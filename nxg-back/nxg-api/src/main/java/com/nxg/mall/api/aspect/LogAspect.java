package com.nxg.mall.api.aspect;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nxg.mall.common.annotation.Log;
import com.nxg.mall.common.utils.IpUtils;
import com.nxg.mall.common.utils.SecurityUtils;
import com.nxg.mall.common.entity.log.OperationLog; // 假设你的实体类在这
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Method;
import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LogAspect {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper; // Spring Boot 默认注入，用于更优雅地处理参数转JSON

    @Pointcut("@annotation(com.nxg.mall.common.annotation.Log)")
    public void logPointCut() {}

    @AfterReturning(pointcut = "logPointCut()", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Object jsonResult) {
        handleLog(joinPoint, null, jsonResult);
    }

    protected void handleLog(final JoinPoint joinPoint, final Exception e, Object jsonResult) {
        try {
            // 1. 获取方法上的注解
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Log controllerLog = method.getAnnotation(Log.class);
            if (controllerLog == null) return;

            // 2. 获取 Request 属性
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return;
            HttpServletRequest request = attributes.getRequest();

            // 3. 构造并填充 OperationLog 实体
            OperationLog opLog = new OperationLog();
            opLog.setOpTime(System.currentTimeMillis());
            opLog.setClientIp(IpUtils.getIpAddr(request));

            // 填充用户信息 (内存操作)
            opLog.setOperatorId(SecurityUtils.getUserId());
            opLog.setOperatorName(SecurityUtils.getUsername());
            opLog.setRole(SecurityUtils.getUserRole());

            // 填充注解配置
            opLog.setActionType(controllerLog.actionType().getCode());
            opLog.setTargetType(controllerLog.targetType().getCode());
            opLog.setDetail(controllerLog.title());

            // 4. 处理请求参数和目标ID提取
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                // 策略：通常 Controller 第一个参数是 ID 或 DTO
                // 这里将第一个参数作为潜在的 targetId 记录
                opLog.setTargetId(args[0].toString());

                if (controllerLog.isSaveRequestData()) {
                    try {
                        // 使用 Jackson 转 JSON 字符串存储
                        String params = objectMapper.writeValueAsString(args);
                        opLog.setRequestParams(params.length() > 2000 ? params.substring(0, 2000) : params);
                    } catch (Exception jsonEx) {
                        opLog.setRequestParams("参数解析失败");
                    }
                }
            }

            // 5. 发送给 Kafka
            // 因为配置了 JsonSerializer，这里直接传 opLog 对象即可
            // 你的代码已经很棒了，直接维持扁平结构发送即可：
            kafkaTemplate.send("nxg_system_audit", opLog.getOperatorId().toString(), JSON.toJSONString(opLog));

            log.debug("已发送操作日志至Kafka: {}", opLog.getDetail());

        } catch (Exception exp) {
            log.error("操作日志记录异常: ", exp);
        }
    }
}
