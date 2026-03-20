/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import com.sesa.salud.entity.enums.EstadoTurno;
import com.sesa.salud.entity.enums.ServicioClinico;
import com.sesa.salud.entity.enums.TipoTurno;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Turno de trabajo asignado a un profesional de salud dentro de una
 * programación mensual de una IPS Nivel II en Colombia.
 */
@Entity
@Table(
    name = "turnos",
    indexes = {
        @Index(name = "idx_turno_personal",        columnList = "personal_id"),
        @Index(name = "idx_turno_programacion_mes", columnList = "programacion_mes_id"),
        @Index(name = "idx_turno_fecha_inicio",     columnList = "fecha_inicio")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Profesional al que se le asigna el turno. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_id", nullable = false)
    private Personal personal;

    /** Mes al que pertenece este turno. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programacion_mes_id", nullable = false)
    private ProgramacionMes programacionMes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServicioClinico servicio;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_turno", nullable = false, length = 20)
    private TipoTurno tipoTurno;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    /** Duración efectiva en horas (calculada por el servicio, no por el cliente). */
    @Column(name = "duracion_horas", nullable = false)
    private Integer duracionHoras;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoTurno estado = EstadoTurno.BORRADOR;

    /** Indica si la fecha de inicio cae en festivo colombiano. */
    @Column(name = "es_festivo", nullable = false)
    @Builder.Default
    private Boolean esFestivo = false;

    @Column(columnDefinition = "TEXT")
    private String notas;

    /** ID del usuario que creó o modificó por última vez este turno. */
    @Column(name = "modificado_por_id")
    private Long modificadoPorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

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
