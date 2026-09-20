package com.warehouseos.exception;

import java.util.Map;

public class InsufficientStockException extends ApiException {

    public InsufficientStockException(String sku, int requested, int available) {
        super(ErrorCode.INSUFFICIENT_STOCK,
                "Not enough stock available for %s: requested %d, available %d"
                        .formatted(sku, requested, available),
                Map.of("sku", sku, "requested", requested, "available", available));
    }
}
