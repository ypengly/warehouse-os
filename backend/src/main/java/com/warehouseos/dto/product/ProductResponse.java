package com.warehouseos.dto.product;

import com.warehouseos.entity.UnitOfMeasure;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String sku,
        String barcode,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        String brand,
        UnitOfMeasure unitOfMeasure,
        BigDecimal costPrice,
        BigDecimal sellingPrice,
        Integer minStockLevel,
        Integer maxStockLevel,
        Integer reorderLevel,
        Integer weightGrams,
        Integer lengthMm,
        Integer widthMm,
        Integer heightMm,
        String imageUrl,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
