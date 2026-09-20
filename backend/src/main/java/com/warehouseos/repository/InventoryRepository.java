package com.warehouseos.repository;

import com.warehouseos.entity.Inventory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long>,
        JpaSpecificationExecutor<Inventory> {

    Optional<Inventory> findByProductIdAndWarehouseIdAndLocationId(
            Long productId, Long warehouseId, Long locationId);

    @Query("select i from Inventory i " +
           "where i.product.id = :productId and i.warehouse.id = :warehouseId " +
           "and i.location is null")
    Optional<Inventory> findUnlocated(@Param("productId") Long productId,
                                      @Param("warehouseId") Long warehouseId);

    /**
     * Row-level lock for every mutating stock path (reserve, receive, transfer,
     * adjust). Concurrent callers serialise on this row instead of racing, and
     * the 5s timeout stops a stuck transaction from blocking a warehouse.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select i from Inventory i where i.id = :id")
    Optional<Inventory> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select i from Inventory i " +
           "where i.product.id = :productId and i.warehouse.id = :warehouseId " +
           "order by i.id")
    List<Inventory> findByProductAndWarehouseForUpdate(@Param("productId") Long productId,
                                                       @Param("warehouseId") Long warehouseId);

    @Query("select coalesce(sum(i.quantityOnHand - i.reservedQuantity), 0) from Inventory i " +
           "where i.product.id = :productId and i.warehouse.id = :warehouseId")
    int totalAvailable(@Param("productId") Long productId, @Param("warehouseId") Long warehouseId);

    List<Inventory> findByWarehouseId(Long warehouseId);
}
