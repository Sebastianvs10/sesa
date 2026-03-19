/**
 * S5: Reconciliación de medicamentos y alergias por atención.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "reconciliacion_atencion", uniqueConstraints = {
        @UniqueConstraint(columnNames = "atencion_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliacionAtencion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atencion_id", nullable = false, unique = true)
    private Atencion atencion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Personal profesional;

    @Column(name = "medicamentos_referidos", columnDefinition = "TEXT")
    private String medicamentosReferidos;

    @Column(name = "medicamentos_hc", columnDefinition = "TEXT")
    private String medicamentosHc;

    @Column(name = "alergias_referidas", columnDefinition = "TEXT")
    private String alergiasReferidas;

    @Column(name = "alergias_hc", columnDefinition = "TEXT")
    private String alergiasHc;

    @Column(name = "reconciliado_at", nullable = false)
    private Instant reconciliadoAt;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (reconciliadoAt == null) reconciliadoAt = Instant.now();
    }
}
