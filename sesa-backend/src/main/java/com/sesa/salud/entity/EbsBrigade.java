/**
 * Entidad EBS: brigada (evento programado con fechas y equipo).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "ebs_brigades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EbsBrigade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "territory_id", nullable = false)
    private EbsTerritory territory;

    @Column(name = "date_start", nullable = false)
    private LocalDate dateStart;

    @Column(name = "date_end", nullable = false)
    private LocalDate dateEnd;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PROGRAMADA";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
