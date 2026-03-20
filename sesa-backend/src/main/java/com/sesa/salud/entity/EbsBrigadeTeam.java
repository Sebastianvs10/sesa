/**
 * Entidad EBS: asignación de profesional al equipo de una brigada.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "ebs_brigade_team", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "brigade_id", "personal_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(EbsBrigadeTeam.BrigadeTeamId.class)
public class EbsBrigadeTeam {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brigade_id", nullable = false)
    private EbsBrigade brigade;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_id", nullable = false)
    private Personal personal;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrigadeTeamId implements Serializable {
        private Long brigade;   // brigade_id
        private Long personal;  // personal_id
    }
}

