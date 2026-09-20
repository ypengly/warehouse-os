package com.warehouseos.dto.product;

import com.warehouseos.entity.UnitOfMeasure;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = 64)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "must be alphanumeric with . _ or -")
        String sku,

        @Size(max = 64)
        @Pattern(regexp = "^[A-Za-z0-9]*$", message = "must be alphanumeric")
        String barcode,

        @NotBlank @Size(max = 200) String name,
        @Size(max = 4000) String description,
        Long categoryId,
        @Size(max = 120) String brand,
        @NotNull UnitOfMeasure unitOfMeasure,

        @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal costPrice,
        @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal sellingPrice,

        @NotNull @Min(0) Integer minStockLevel,
        @Min(0) Integer maxStockLevel,
        @NotNull @Min(0) Integer reorderLevel,

        @Min(0) Integer weightGrams,
        @Min(0) Integer lengthMm,
        @Min(0) Integer widthMm,
        @Min(0) Integer heightMm,
        @Size(max = 500) String imageUrl,
        Boolean active
) {}
