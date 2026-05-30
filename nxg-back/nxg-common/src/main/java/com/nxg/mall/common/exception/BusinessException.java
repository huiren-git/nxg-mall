package com.nxg.mall.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;

    /**
     * 默认错误码为 400
     */
    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    /**
     * 支持自定义错误码
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}