package com.warehouseos.mapper;

import com.warehouseos.dto.warehouse.WarehouseResponse;
import com.warehouseos.entity.Warehouse;
import org.springframework.stereotype.Component;

@Component
public class WarehouseMapper {

    public WarehouseResponse toResponse(Warehouse w) {
        return new WarehouseResponse(
                w.getId(), w.getCode(), w.getName(), w.getAddressLine(), w.getCity(),
                w.getCountry(), w.getPostalCode(), w.getContactName(), w.getContactEmail(),
                w.getContactPhone(),
                w.getManager() != null ? w.getManager().getId() : null,
                w.getManager() != null ? w.getManager().getFullName() : null,
                w.getStatus(), w.getCapacityUnits(), w.getCreatedAt(), w.getUpdatedAt());
    }
}
