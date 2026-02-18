/**
 * Entidad Hospitalización
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "hospitalizaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hospitalizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Column(length = 120)
    private String servicio;

    @Column(length = 50)
    private String cama;

    @Column(length = 50)
    @Builder.Default
    private String estado = "INGRESADO";

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDateTime fechaIngreso;

    @Column(name = "fecha_egreso")
    private LocalDateTime fechaEgreso;

    @Column(name = "evolucion_diaria", columnDefinition = "TEXT")
    private String evolucionDiaria;

    @Column(name = "ordenes_medicas", columnDefinition = "TEXT")
    private String ordenesMedicas;

    @Column(columnDefinition = "TEXT")
    private String epicrisis;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (fechaIngreso == null) fechaIngreso = LocalDateTime.now();
    }
}
