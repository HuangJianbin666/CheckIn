package com.example.checkin.common;

import java.time.Instant;

/**
 * 统一响应结构。data 在失败时为 null。
 */
public record ApiResponse<T>(
        String code,
        String message,
        T data,
        String requestId,
        Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ErrorCode.OK.code(), ErrorCode.OK.message(), data,
                RequestContext.requestId(), Instant.now());
    }

    public static ApiResponse<Void> ok() {
        return ok(null);
    }

    public static ApiResponse<Void> error(ErrorCode error) {
        return new ApiResponse<>(error.code(), error.message(), null,
                RequestContext.requestId(), Instant.now());
    }

    public static ApiResponse<Void> error(ErrorCode error, String message) {
        return new ApiResponse<>(error.code(), message, null,
                RequestContext.requestId(), Instant.now());
    }
}