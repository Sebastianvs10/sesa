/**
 * Entidad Dolor - Registro de dolores del paciente
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "dolores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dolor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "historia_clinica_id")
    private HistoriaClinica historiaClinica;

    @Column(name = "zona_corporal", nullable = false, length = 60)
    private String zonaCorporal;

    @Column(name = "zona_label", nullable = false, length = 120)
    private String zonaLabel;

    @Column(name = "tipo_dolor", length = 40)
    private String tipoDolor;

    @Column(nullable = false)
    private Integer intensidad;

    @Column(length = 20)
    @Builder.Default
    private String severidad = "leve";

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "activo";

    @Column(name = "fecha_inicio", nullable = false)
    private Instant fechaInicio;

    @Column(name = "fecha_resolucion")
    private Instant fechaResolucion;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "factores_agravantes", columnDefinition = "TEXT")
    private String factoresAgravantes;

    @Column(name = "factores_aliviantes", columnDefinition = "TEXT")
    private String factoresAliviantes;

    @Column(columnDefinition = "TEXT")
    private String tratamiento;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Column(length = 10)
    @Builder.Default
    private String vista = "front";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (fechaInicio == null) {
            fechaInicio = Instant.now();
        }
    }
}
