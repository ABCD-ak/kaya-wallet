package com.kaya.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Unified response envelope used by the frontend:
 * {@code { code, message, requestId, data }}. {@code code = 0} means success.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    public static final int SUCCESS_CODE = 0;

    private final int code;
    private final String message;
    private final String requestId;
    private final T data;

    private ApiResponse(int code, String message, String requestId, T data) {
        this.code = code;
        this.message = message;
        this.requestId = requestId;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(SUCCESS_CODE, "ok", currentRequestId(), data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, currentRequestId(), null);
    }

    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return new ApiResponse<>(code, message, currentRequestId(), data);
    }

    private static String currentRequestId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            Object requestId = servletAttributes.getRequest()
                    .getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
            return requestId == null ? null : requestId.toString();
        }
        return null;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getRequestId() {
        return requestId;
    }

    public T getData() {
        return data;
    }
}
