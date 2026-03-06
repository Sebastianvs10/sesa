/**
 * Entidad EBS: microterritorio asignable a equipos APS.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "ebs_territories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EbsTerritory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 50)
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_territory_id")
    private EbsTerritory parentTerritory;

    @Column(columnDefinition = "TEXT")
    private String geometry;

    @Column(name = "assigned_team_id")
    private Long assignedTeamId;

    /** Código DANE departamento (IGAC). */
    @Column(name = "igac_departamento_codigo", length = 2)
    private String igacDepartamentoCodigo;

    /** Código DANE municipio (IGAC). */
    @Column(name = "igac_municipio_codigo", length = 5)
    private String igacMunicipioCodigo;

    /** Código vereda (IGAC). */
    @Column(name = "igac_vereda_codigo", length = 20)
    private String igacVeredaCodigo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
