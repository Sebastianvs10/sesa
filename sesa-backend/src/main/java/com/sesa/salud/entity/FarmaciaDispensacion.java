/**
 * Entidad Dispensación de Farmacia
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "farmacia_dispensaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmaciaDispensacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private FarmaciaMedicamento medicamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "fecha_dispensacion", nullable = false)
    private LocalDateTime fechaDispensacion;

    @Column(name = "entregado_por", length = 150)
    private String entregadoPor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (fechaDispensacion == null) fechaDispensacion = LocalDateTime.now();
    }
}
