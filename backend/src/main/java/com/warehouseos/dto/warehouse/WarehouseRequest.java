package com.warehouseos.dto.warehouse;

import jakarta.validation.constraints.*;

public record WarehouseRequest(
        @NotBlank @Size(max = 32)
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "must be alphanumeric with hyphens")
        String code,

        @NotBlank @Size(max = 160) String name,
        @Size(max = 255) String addressLine,
        @Size(max = 120) String city,
        @Size(max = 120) String country,
        @Size(max = 32) String postalCode,
        @Size(max = 160) String contactName,
        @Email @Size(max = 160) String contactEmail,
        @Size(max = 48) String contactPhone,
        Long managerId,
        @Min(0) Integer capacityUnits
) {}
