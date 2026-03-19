/**
 * Trazabilidad de lectura de resultado crítico (S2).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "resultado_critico_lectura",
       uniqueConstraints = @UniqueConstraint(columnNames = { "orden_clinica_id", "personal_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoCriticoLectura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_clinica_id", nullable = false)
    private OrdenClinica ordenClinica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_id", nullable = false)
    private Personal personal;

    @Column(name = "leido_at", nullable = false)
    private Instant leidoAt;

    @PrePersist
    protected void onCreate() {
        if (leidoAt == null) {
            leidoAt = Instant.now();
        }
    }
}
