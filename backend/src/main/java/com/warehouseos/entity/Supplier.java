package com.warehouseos.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
public class Supplier extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 32)
    private String code;

    @Column(name = "company_name", nullable = false, length = 180)
    private String companyName;

    @Column(name = "contact_person", length = 160)
    private String contactPerson;

    @Column(name = "email", length = 160)
    private String email;

    @Column(name = "phone", length = 48)
    private String phone;

    @Column(name = "address_line", length = 255)
    private String addressLine;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "country", length = 120)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private EntityStatus status = EntityStatus.ACTIVE;
}
