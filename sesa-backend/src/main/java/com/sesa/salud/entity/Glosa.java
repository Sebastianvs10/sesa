/**
 * S9: Glosa — rechazo de factura con motivo, estado y seguimiento.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "glosas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Glosa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;

    @Column(name = "motivo_rechazo", nullable = false, columnDefinition = "TEXT")
    private String motivoRechazo;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String estado = "PENDIENTE";

    @Column(name = "fecha_registro", nullable = false)
    private Instant fechaRegistro;

    @Column(name = "fecha_respuesta")
    private Instant fechaRespuesta;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_id")
    private Usuario creadoPor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "glosa", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GlosaAdjunto> adjuntos = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (fechaRegistro == null) fechaRegistro = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
