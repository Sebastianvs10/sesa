/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import com.sesa.salud.entity.enums.EstadoProgramacion;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa el estado y los metadatos de la programación de turnos
 * para un mes calendario dentro de una sede (tenant).
 *
 * <p>Existe una sola instancia por (anio, mes) por tenant.
 * Los turnos individuales apuntan a esta entidad.</p>
 */
@Entity
@Table(
    name = "programacion_mes",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_programacion_anio_mes",
        columnNames = {"anio", "mes"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramacionMes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Año calendario (ej. 2025). */
    @Column(nullable = false)
    private Integer anio;

    /** Mes (1 = enero … 12 = diciembre). */
    @Column(nullable = false)
    private Integer mes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoProgramacion estado = EstadoProgramacion.BORRADOR;

    /** ID del usuario que creó la programación. */
    @Column(name = "creado_por_id")
    private Long creadoPorId;

    @Column(name = "creado_por_nombre", length = 200)
    private String creadoPorNombre;

    /** ID del Coordinador Médico que aprobó la programación. */
    @Column(name = "aprobado_por_id")
    private Long aprobadoPorId;

    @Column(name = "aprobado_por_nombre", length = 200)
    private String aprobadoPorNombre;

    @Column(name = "fecha_aprobacion")
    private Instant fechaAprobacion;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "programacionMes", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Turno> turnos = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
