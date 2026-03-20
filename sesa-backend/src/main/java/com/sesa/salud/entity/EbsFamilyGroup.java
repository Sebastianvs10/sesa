/**
 * Entidad EBS: grupo familiar vinculado a un hogar.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "ebs_family_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EbsFamilyGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private EbsHousehold household;

    @Column(name = "fhir_group_id", length = 64)
    private String fhirGroupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_contact_patient_id")
    private Paciente mainContactPatient;

    @Column(name = "socioeconomic_level", length = 30)
    private String socioeconomicLevel;

    @Column(name = "risk_notes", columnDefinition = "TEXT")
    private String riskNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
