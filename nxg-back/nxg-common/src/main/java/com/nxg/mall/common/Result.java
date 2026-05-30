package com.nxg.mall.common;

import lombok.Data;

@Data
public class Result<T> {

    private Integer code;   // 状态码
    private String message; // 提示信息
    private T data;         // 数据

    // ================= 构造方法 =================
    public Result() {}

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ================= 成功 =================
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    // ================= 失败 =================
    public static <T> Result<T> error(String message) {
        return new Result<>(400, message, null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}