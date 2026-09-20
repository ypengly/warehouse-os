package com.warehouseos.exception;

import java.util.Map;

public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String resource, String field, Object value) {
        super(ErrorCode.DUPLICATE_RESOURCE,
                "%s with %s '%s' already exists".formatted(resource, field, value),
                Map.of("resource", resource, "field", field, "value", String.valueOf(value)));
    }
}
