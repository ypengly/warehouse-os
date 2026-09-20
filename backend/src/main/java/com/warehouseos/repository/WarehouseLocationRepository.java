package com.warehouseos.repository;

import com.warehouseos.entity.WarehouseLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocation, Long> {

    Page<WarehouseLocation> findByWarehouseId(Long warehouseId, Pageable pageable);

    Optional<WarehouseLocation> findByWarehouseIdAndCodeIgnoreCase(Long warehouseId, String code);

    boolean existsByWarehouseIdAndCodeIgnoreCase(Long warehouseId, String code);
}
