package com.warehouseos.controller;

import com.warehouseos.dto.common.PageResponse;
import com.warehouseos.dto.warehouse.WarehouseRequest;
import com.warehouseos.dto.warehouse.WarehouseResponse;
import com.warehouseos.entity.EntityStatus;
import com.warehouseos.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Warehouses")
@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @Operation(summary = "List warehouses")
    @GetMapping
    public PageResponse<WarehouseResponse> list(
            @PageableDefault(size = 25, sort = "code", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return warehouseService.list(pageable);
    }

    @Operation(summary = "Fetch a single warehouse")
    @GetMapping("/{id}")
    public WarehouseResponse getById(@PathVariable Long id) {
        return warehouseService.getById(id);
    }

    @Operation(summary = "Create a warehouse (ADMIN)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseResponse create(@Valid @RequestBody WarehouseRequest request) {
        return warehouseService.create(request);
    }

    @Operation(summary = "Update a warehouse (ADMIN)")
    @PutMapping("/{id}")
    public WarehouseResponse update(@PathVariable Long id,
                                    @Valid @RequestBody WarehouseRequest request) {
        return warehouseService.update(id, request);
    }

    @Operation(summary = "Activate or deactivate a warehouse (ADMIN)")
    @PatchMapping("/{id}/status")
    public WarehouseResponse setStatus(@PathVariable Long id,
                                       @RequestParam EntityStatus status) {
        return warehouseService.setStatus(id, status);
    }
}
