/**
 * Catálogo CUPS (Clasificación Única de Procedimientos en Salud) Colombia.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cups_catalogo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CupCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(length = 100)
    private String capitulo;

    @Column(name = "tipo_servicio", nullable = false, length = 80)
    @Builder.Default
    private String tipoServicio = "PROCEDIMIENTO";

    @Column(name = "precio_sugerido", precision = 14, scale = 2)
    private BigDecimal precioSugerido;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
