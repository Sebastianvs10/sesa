/**
 * Radicación de factura ante EPS — registro de envío y seguimiento (normativa 22 d hábiles).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "radicaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Radicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;

    @Column(name = "fecha_radicacion", nullable = false)
    private Instant fechaRadicacion;

    @Column(name = "numero_radicado", length = 80)
    private String numeroRadicado;

    @Column(name = "eps_codigo", length = 20)
    private String epsCodigo;

    @Column(name = "eps_nombre", length = 200)
    private String epsNombre;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String estado = "RADICADA";

    @Column(name = "cuv", length = 100)
    private String cuv;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
