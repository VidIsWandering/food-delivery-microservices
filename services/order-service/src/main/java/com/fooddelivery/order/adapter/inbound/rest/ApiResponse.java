package com.fooddelivery.order.adapter.inbound.rest;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Standard API response wrapper.
 * All endpoints MUST return this format.
 *
 * @see docs/development/API_STYLE_GUIDE.md
 *
 * Success: { "success": true, "data": {...}, "meta": {"timestamp": "..."} }
 * Error:   { "success": false, "error": {"code": "...", "message": "...", "details": [...]}, "meta": {...} }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    T data,
    ErrorBody error,
    Meta meta
) {
    // --- Factory methods ---

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, new Meta(Instant.now().toString(), null));
    }

    public static <T> ApiResponse<T> ok(T data, Pagination pagination) {
        return new ApiResponse<>(true, data, null, new Meta(Instant.now().toString(), pagination));
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null,
            new ErrorBody(code, message, null),
            new Meta(Instant.now().toString(), null));
    }

    public static <T> ApiResponse<T> error(String code, String message, java.util.List<FieldError> details) {
        return new ApiResponse<>(false, null,
            new ErrorBody(code, message, details),
            new Meta(Instant.now().toString(), null));
    }

    // --- Inner records ---

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorBody(
        String code,
        String message,
        java.util.List<FieldError> details
    ) {}

    public record FieldError(
        String field,
        String message
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Meta(
        String timestamp,
        Pagination pagination
    ) {}

    public record Pagination(
        int page,
        int size,
        long totalItems,
        int totalPages
    ) {}
}
