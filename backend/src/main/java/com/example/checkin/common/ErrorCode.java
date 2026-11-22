package com.example.checkin.common;

import org.springframework.http.HttpStatus;

/**
 * 稳定机器可读错误码。HTTP 状态表达错误类别，code 供前端分支判断。
 */
public enum ErrorCode {
    OK("OK", "success", HttpStatus.OK),
    VALIDATION_ERROR("VALIDATION_ERROR", "请求参数校验失败", HttpStatus.BAD_REQUEST),
    MALFORMED_JSON("MALFORMED_JSON", "请求体格式错误", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED("UNAUTHENTICATED", "未登录或登录已过期", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "用户名或密码错误", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("ACCESS_DENIED", "无权访问", HttpStatus.FORBIDDEN),
    HABIT_NOT_FOUND("HABIT_NOT_FOUND", "打卡项不存在", HttpStatus.NOT_FOUND),
    ENDPOINT_NOT_FOUND("ENDPOINT_NOT_FOUND", "接口不存在", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "请求方法不允许", HttpStatus.METHOD_NOT_ALLOWED),
    USERNAME_EXISTS("USERNAME_EXISTS", "用户名已存在", HttpStatus.CONFLICT),
    AUTH_SERVICE_UNAVAILABLE("AUTH_SERVICE_UNAVAILABLE", "认证服务不可用", HttpStatus.SERVICE_UNAVAILABLE),
    DATABASE_UNAVAILABLE("DATABASE_UNAVAILABLE", "数据库不可用", HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL_ERROR("INTERNAL_ERROR", "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public HttpStatus status() {
        return status;
    }
}