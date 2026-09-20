package com.warehouseos.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "warehouse_locations")
@Getter
@Setter
@NoArgsConstructor
public class WarehouseLocation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    /** Human-readable bin code, e.g. A-01-01. Unique within a warehouse. */
    @Column(name = "code", nullable = false, length = 48)
    private String code;

    @Column(name = "zone", length = 16)
    private String zone;

    @Column(name = "aisle", length = 16)
    private String aisle;

    @Column(name = "rack", length = 16)
    private String rack;

    @Column(name = "shelf", length = 16)
    private String shelf;

    @Column(name = "bin", length = 16)
    private String bin;

    @Column(name = "capacity_units")
    private Integer capacityUnits;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private EntityStatus status = EntityStatus.ACTIVE;
}
