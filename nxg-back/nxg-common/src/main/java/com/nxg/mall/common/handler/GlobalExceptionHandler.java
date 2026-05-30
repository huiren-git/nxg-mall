package com.nxg.mall.common.handler;

import com.nxg.mall.common.Result;
import com.nxg.mall.common.exception.BusinessException; // 引入刚才定义的异常
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 新增：处理自定义业务异常 (BusinessException)
     * 专门用于处理支付失败、余额不足、库存不足等业务逻辑问题
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        // 直接返回异常中携带的错误码和错误消息
        return Result.error(e.getCode(), e.getMessage());
    }

    // 处理参数校验失败异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("参数校验失败");
        return Result.error(400, message);
    }

    // 处理通用运行时异常
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        return Result.error(e.getMessage() != null ? e.getMessage() : "系统繁忙，请稍后再试");
    }

    // 处理所有未捕获的异常
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        e.printStackTrace();
        return Result.error(500, "服务器内部错误: " + e.getMessage());
    }
}