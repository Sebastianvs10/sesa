/**
 * S10: Cuestionario pre-consulta (ePRO) — datos aportados por el paciente antes de la cita.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "cuestionario_preconsulta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuestionarioPreconsulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id", nullable = false)
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Column(name = "motivo_palabras", columnDefinition = "TEXT")
    private String motivoPalabras;

    @Column(name = "dolor_eva")
    private Integer dolorEva;

    @Column(name = "ansiedad_eva")
    private Integer ansiedadEva;

    @Column(name = "medicamentos_actuales", columnDefinition = "TEXT")
    private String medicamentosActuales;

    @Column(name = "alergias_referidas", columnDefinition = "TEXT")
    private String alergiasReferidas;

    @Column(name = "enviado_at", nullable = false)
    private Instant enviadoAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (enviadoAt == null) enviadoAt = Instant.now();
    }
}
