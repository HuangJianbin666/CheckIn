package com.example.checkin.common;

import java.time.Instant;
import java.util.UUID;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 请求上下文工具。requestId 由 {@link RequestIdFilter} 写入请求属性，此处读取；
 * 无请求上下文（单元测试）时回退为新建 UUID。
 */
public final class RequestContext {

    public static final String REQUEST_ID_ATTR = "checkin.requestId";

    private RequestContext() {
    }

    public static String requestId() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet
                && servlet.getRequest() != null) {
            Object value = servlet.getRequest().getAttribute(REQUEST_ID_ATTR);
            if (value != null) {
                return value.toString();
            }
        }
        return UUID.randomUUID().toString();
    }
}