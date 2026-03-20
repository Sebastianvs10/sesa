/**
 * Evolución y seguimiento post-tratamiento odontológico.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "evoluciones_odontologicas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EvolucionOdontologica {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Personal profesional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id", nullable = false)
    private ConsultaOdontologica consulta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private PlanTratamiento plan;

    @Column(name = "nota_evolucion", columnDefinition = "TEXT", nullable = false)
    private String notaEvolucion;

    @Column(name = "control_post_tratamiento", columnDefinition = "TEXT")
    private String controlPostTratamiento;

    @Column(name = "proxima_cita_recomendada")
    private Instant proximaCitaRecomendada;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() { createdAt = Instant.now(); }
}
