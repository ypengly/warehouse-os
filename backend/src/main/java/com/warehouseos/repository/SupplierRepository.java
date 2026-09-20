package com.warehouseos.repository;

import com.warehouseos.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long>,
        JpaSpecificationExecutor<Supplier> {
    Optional<Supplier> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
}
