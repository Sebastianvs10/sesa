/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "citas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_id", nullable = false)
    private Personal profesional;

    @Column(nullable = false, length = 100)
    private String servicio;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(length = 50)
    @Builder.Default
    private String estado = "AGENDADA";

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Column(name = "motivo_cancelacion", columnDefinition = "TEXT")
    private String motivoCancelacion;

    // Campos normativos Res. 2953/2014 (oportunidad en asignación de citas)
    @Column(name = "tipo_cita", length = 20)
    private String tipoCita;

    @Column(name = "numero_autorizacion_eps", length = 60)
    private String numeroAutorizacionEps;

    @Column(name = "duracion_estimada_min")
    private Integer duracionEstimadaMin;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
