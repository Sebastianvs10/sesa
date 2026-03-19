/**
 * S15: Registro de visualización de una sugerencia GPC (auditoría).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "gpc_sugerencia_mostrada")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GpcSugerenciaMostrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "atencion_id", nullable = false)
    private Long atencionId;

    @Column(name = "codigo_cie10", nullable = false, length = 20)
    private String codigoCie10;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guia_id", nullable = false)
    private GuiaGpc guia;

    @Column(name = "mostrado_at", nullable = false)
    private Instant mostradoAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesional_id")
    private Personal profesional;

    @PrePersist
    protected void onCreate() {
        if (mostradoAt == null) mostradoAt = Instant.now();
    }
}
