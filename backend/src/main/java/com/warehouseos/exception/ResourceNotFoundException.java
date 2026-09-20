package com.warehouseos.exception;

import java.util.Map;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resource, Object identifier) {
        super(ErrorCode.RESOURCE_NOT_FOUND,
                "%s not found: %s".formatted(resource, identifier),
                Map.of("resource", resource, "identifier", String.valueOf(identifier)));
    }
}
