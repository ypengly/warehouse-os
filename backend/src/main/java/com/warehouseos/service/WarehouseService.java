package com.warehouseos.service;

import com.warehouseos.dto.common.PageResponse;
import com.warehouseos.dto.warehouse.WarehouseRequest;
import com.warehouseos.dto.warehouse.WarehouseResponse;
import com.warehouseos.entity.EntityStatus;
import com.warehouseos.entity.RoleName;
import com.warehouseos.entity.User;
import com.warehouseos.entity.Warehouse;
import com.warehouseos.exception.BusinessRuleException;
import com.warehouseos.exception.DuplicateResourceException;
import com.warehouseos.exception.ResourceNotFoundException;
import com.warehouseos.mapper.WarehouseMapper;
import com.warehouseos.repository.UserRepository;
import com.warehouseos.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final WarehouseMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<WarehouseResponse> list(Pageable pageable) {
        return PageResponse.from(warehouseRepository.findAll(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getById(Long id) {
        return mapper.toResponse(find(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public WarehouseResponse create(WarehouseRequest request) {
        if (warehouseRepository.existsByCodeIgnoreCase(request.code())) {
            throw new DuplicateResourceException("Warehouse", "code", request.code());
        }
        Warehouse warehouse = new Warehouse();
        apply(request, warehouse);
        return mapper.toResponse(warehouseRepository.save(warehouse));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public WarehouseResponse update(Long id, WarehouseRequest request) {
        Warehouse warehouse = find(id);
        if (!warehouse.getCode().equalsIgnoreCase(request.code())
                && warehouseRepository.existsByCodeIgnoreCase(request.code())) {
            throw new DuplicateResourceException("Warehouse", "code", request.code());
        }
        apply(request, warehouse);
        return mapper.toResponse(warehouse);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public WarehouseResponse setStatus(Long id, EntityStatus status) {
        Warehouse warehouse = find(id);
        warehouse.setStatus(status);
        return mapper.toResponse(warehouse);
    }

    private void apply(WarehouseRequest request, Warehouse warehouse) {
        warehouse.setCode(request.code().toUpperCase());
        warehouse.setName(request.name());
        warehouse.setAddressLine(request.addressLine());
        warehouse.setCity(request.city());
        warehouse.setCountry(request.country());
        warehouse.setPostalCode(request.postalCode());
        warehouse.setContactName(request.contactName());
        warehouse.setContactEmail(request.contactEmail());
        warehouse.setContactPhone(request.contactPhone());
        warehouse.setCapacityUnits(request.capacityUnits());
        warehouse.setManager(resolveManager(request.managerId()));
    }

    private User resolveManager(Long managerId) {
        if (managerId == null) {
            return null;
        }
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", managerId));
        if (!manager.hasRole(RoleName.MANAGER) && !manager.hasRole(RoleName.ADMIN)) {
            throw new BusinessRuleException(
                    "Only users with the MANAGER or ADMIN role can manage a warehouse");
        }
        return manager;
    }

    private Warehouse find(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", id));
    }
}
