package com.warehouseos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Stock held for a product at a warehouse (optionally pinned to a bin location).
 *
 * All mutations go through the domain methods below so the invariants
 *   on_hand >= 0, reserved >= 0, reserved <= on_hand
 * can never be bypassed. The same rules are mirrored as CHECK constraints in
 * the schema, giving a second line of defence against bad writes.
 */
@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
public class Inventory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private WarehouseLocation location;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand = 0;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity = 0;

    @Column(name = "damaged_quantity", nullable = false)
    private int damagedQuantity = 0;

    /** Overrides the product-level reorder level for this warehouse if set. */
    @Column(name = "reorder_level")
    private Integer reorderLevel;

    @Column(name = "last_movement_at")
    private Instant lastMovementAt;

    @Transient
    public int getAvailableQuantity() {
        return quantityOnHand - reservedQuantity;
    }

    public int effectiveReorderLevel() {
        return reorderLevel != null ? reorderLevel : product.getReorderLevel();
    }

    // ---- domain mutations (package-visible guards, see InventoryService) ----

    public void increaseOnHand(int quantity) {
        requirePositive(quantity);
        this.quantityOnHand += quantity;
        this.lastMovementAt = Instant.now();
    }

    /** Decreases physical stock. Callers must have already released reservations. */
    public void decreaseOnHand(int quantity) {
        requirePositive(quantity);
        if (quantity > getAvailableQuantity()) {
            throw new IllegalStateException("Insufficient available stock");
        }
        this.quantityOnHand -= quantity;
        this.lastMovementAt = Instant.now();
    }

    public void reserve(int quantity) {
        requirePositive(quantity);
        if (quantity > getAvailableQuantity()) {
            throw new IllegalStateException("Insufficient available stock to reserve");
        }
        this.reservedQuantity += quantity;
    }

    public void releaseReservation(int quantity) {
        requirePositive(quantity);
        if (quantity > reservedQuantity) {
            throw new IllegalStateException("Cannot release more than is reserved");
        }
        this.reservedQuantity -= quantity;
    }

    /** Consumes reserved stock: removes it from both reserved and on-hand. */
    public void consumeReservation(int quantity) {
        requirePositive(quantity);
        if (quantity > reservedQuantity) {
            throw new IllegalStateException("Cannot consume more than is reserved");
        }
        this.reservedQuantity -= quantity;
        this.quantityOnHand -= quantity;
        this.lastMovementAt = Instant.now();
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive, got " + quantity);
        }
    }
}
