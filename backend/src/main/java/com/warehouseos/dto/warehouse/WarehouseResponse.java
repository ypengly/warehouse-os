package com.warehouseos.dto.warehouse;

import com.warehouseos.entity.EntityStatus;

import java.time.Instant;

public record WarehouseResponse(
        Long id,
        String code,
        String name,
        String addressLine,
        String city,
        String country,
        String postalCode,
        String contactName,
        String contactEmail,
        String contactPhone,
        Long managerId,
        String managerName,
        EntityStatus status,
        Integer capacityUnits,
        Instant createdAt,
        Instant updatedAt
) {}
