/**
 * Entidad EBS: hogar georreferenciado dentro de un microterritorio.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ebs_households")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EbsHousehold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "territory_id", nullable = false)
    private EbsTerritory territory;

    @Column(name = "fhir_location_id", length = 64)
    private String fhirLocationId;

    @Column(name = "address_text", length = 255)
    private String addressText;

    @Column(precision = 10, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 6)
    private BigDecimal longitude;

    @Column(name = "rural")
    private Boolean rural;

    @Column(length = 20)
    private String stratum;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String state = "PENDIENTE_VISITA";

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
