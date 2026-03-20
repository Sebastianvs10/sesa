/**
 * S15: Entidad Guía de práctica clínica (GPC) por código CIE-10.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "guia_gpc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuiaGpc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_cie10", nullable = false, length = 20)
    private String codigoCie10;

    @Column(nullable = false, length = 300)
    private String titulo;

    @Column(name = "criterios_control", columnDefinition = "TEXT")
    private String criteriosControl;

    @Column(name = "medicamentos_primera_linea", columnDefinition = "TEXT")
    private String medicamentosPrimeraLinea;

    @Column(name = "estudios_seguimiento", columnDefinition = "TEXT")
    private String estudiosSeguimiento;

    @Column(length = 200)
    private String fuente;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
