package com.warehouseos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Append-only ledger row. There are deliberately no setters used after
 * persistence and no repository delete/update path; the database also refuses
 * UPDATE and DELETE on this table via a trigger.
 */
@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false, updatable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", updatable = false)
    private WarehouseLocation location;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, updatable = false, length = 24)
    private MovementType movementType;

    /** Signed delta applied to quantity_on_hand. */
    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantity;

    @Column(name = "previous_quantity", nullable = false, updatable = false)
    private int previousQuantity;

    @Column(name = "new_quantity", nullable = false, updatable = false)
    private int newQuantity;

    @Column(name = "reference_type", updatable = false, length = 32)
    private String referenceType;

    @Column(name = "reference_number", updatable = false, length = 64)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by", updatable = false)
    private User performedBy;

    @Column(name = "notes", updatable = false, length = 500)
    private String notes;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt = Instant.now();
}
