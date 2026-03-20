/**
 * Plan de tratamiento odontológico por fases con seguimiento de abonos.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "planes_tratamiento")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanTratamiento {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Personal profesional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id")
    private ConsultaOdontologica consulta;

    @Column(nullable = false, length = 200)
    @Builder.Default
    private String nombre = "Plan de Tratamiento";

    @Column(nullable = false)
    @Builder.Default
    private Integer fase = 1;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "valor_total", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(name = "valor_final", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal valorFinal = BigDecimal.ZERO;

    @Column(name = "valor_abonado", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal valorAbonado = BigDecimal.ZERO;

    /** EPS | PARTICULAR | MIXTO */
    @Column(name = "tipo_pago", length = 20)
    @Builder.Default
    private String tipoPago = "PARTICULAR";

    /** PENDIENTE | EN_TRATAMIENTO | FINALIZADO | CANCELADO */
    @Column(length = 30)
    @Builder.Default
    private String estado = "PENDIENTE";

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PlanTratamientoItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = Instant.now(); updatedAt = Instant.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = Instant.now(); }
}
