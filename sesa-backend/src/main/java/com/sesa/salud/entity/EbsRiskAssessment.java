/**
 * Entidad EBS: valoración de riesgo (cardiovascular, materno, crónicos).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ebs_risk_assessments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EbsRiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Paciente patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_visit_id")
    private EbsHomeVisit homeVisit;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "fhir_observation_id", length = 64)
    private String fhirObservationId;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
