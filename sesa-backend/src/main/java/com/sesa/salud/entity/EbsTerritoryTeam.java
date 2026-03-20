/**
 * Entidad EBS: asignación de profesional al equipo de un territorio.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "ebs_territory_team", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "territory_id", "personal_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(EbsTerritoryTeam.TerritoryTeamId.class)
public class EbsTerritoryTeam {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "territory_id", nullable = false)
    private EbsTerritory territory;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_id", nullable = false)
    private Personal personal;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TerritoryTeamId implements Serializable {
        private Long territory;  // territory_id
        private Long personal;   // personal_id
    }
}

