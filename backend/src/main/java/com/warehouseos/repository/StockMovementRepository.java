package com.warehouseos.repository;

import com.warehouseos.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

/**
 * Intentionally NOT a JpaRepository: the ledger exposes save and read only,
 * so no caller can reach delete() or deleteAll().
 */
public interface StockMovementRepository extends Repository<StockMovement, Long>,
        JpaSpecificationExecutor<StockMovement> {

    StockMovement save(StockMovement movement);

    Page<StockMovement> findByProductId(Long productId, Pageable pageable);

    Page<StockMovement> findByWarehouseId(Long warehouseId, Pageable pageable);
}
