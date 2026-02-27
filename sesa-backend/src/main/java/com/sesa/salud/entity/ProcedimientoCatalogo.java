/**
 * Catálogo de procedimientos odontológicos (CUPS + personalizados).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "procedimientos_catalogo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProcedimientoCatalogo {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30)
    private String codigo;

    @Column(nullable = false, length = 250)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(length = 100)
    private String categoria;

    @Column(name = "precio_base", precision = 12, scale = 2)
    private BigDecimal precioBase;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    /** CUPS (Colombia) | PERSONALIZADO */
    @Column(length = 20)
    @Builder.Default
    private String origen = "PERSONALIZADO";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() { createdAt = Instant.now(); }
}
