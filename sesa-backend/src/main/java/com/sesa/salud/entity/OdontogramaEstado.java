/**
 * Estado de pieza dental y superficie (sistema FDI).
 * Un registro por pieza+superficie para historial completo.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "odontograma_estado",
       indexes = { @Index(name = "idx_odont_paciente", columnList = "paciente_id"),
                   @Index(name = "idx_odont_pieza",    columnList = "paciente_id, pieza_fdi") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OdontogramaEstado {

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

    /** Número FDI (11-18, 21-28, 31-38, 41-48, 51-55, 61-65, 71-75, 81-85) */
    @Column(name = "pieza_fdi", nullable = false)
    private Integer piezaFdi;

    /** MESIAL | DISTAL | VESTIBULAR | LINGUAL | OCLUSAL | GENERAL */
    @Column(length = 20, nullable = false)
    private String superficie;

    /**
     * SANO | CARIES | OBTURACION | ENDODONCIA | CORONA | AUSENTE |
     * PROTESIS | FRACTURA | SELLANTE | EXTRACCION_INDICADA | IMPLANTE
     */
    @Column(length = 40, nullable = false)
    private String estado;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() { createdAt = Instant.now(); }
}
