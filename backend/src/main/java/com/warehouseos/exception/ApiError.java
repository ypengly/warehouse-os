package com.warehouseos.exception;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The single error shape returned by every endpoint.
 * Messages are deliberately free of stack traces and internal identifiers.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, Object> details,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {}

    public static ApiError of(ErrorCode code, String message, String path,
                              Map<String, Object> details, List<FieldError> fieldErrors) {
        return new ApiError(Instant.now(), code.status().value(), code.name(),
                message, path,
                details == null || details.isEmpty() ? null : details,
                fieldErrors == null || fieldErrors.isEmpty() ? null : fieldErrors);
    }
}
