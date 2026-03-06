/**
 * Entidad EBS: visita domiciliaria (historia clínica comunitaria).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "ebs_home_visits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EbsHomeVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private EbsHousehold household;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_group_id")
    private EbsFamilyGroup familyGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id")
    private Personal professional;

    @Column(name = "visit_date", nullable = false)
    private Instant visitDate;

    @Column(name = "visit_type", length = 50)
    private String visitType;

    @Column(name = "tipo_intervencion", length = 80)
    private String tipoIntervencion;

    @Column(name = "vereda_codigo", length = 20)
    private String veredaCodigo;

    @Column(name = "diagnostico_cie10", length = 20)
    private String diagnosticoCie10;

    @Column(name = "plan_cuidado", columnDefinition = "TEXT")
    private String planCuidado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brigade_id")
    private EbsBrigade brigade;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "fhir_encounter_id", length = 64)
    private String fhirEncounterId;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "EN_PROCESO";

    @Column(name = "offline_uuid", length = 64)
    private String offlineUuid;

    @Column(name = "sync_status", nullable = false, length = 20)
    @Builder.Default
    private String syncStatus = "SYNCED";

    @Column(name = "sync_errors", columnDefinition = "TEXT")
    private String syncErrors;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
