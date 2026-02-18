/**
 * Entidad Nota de Enfermería
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notas_enfermeria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotaEnfermeria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atencion_id", nullable = false)
    private Atencion atencion;

    @Column(name = "nota", nullable = false, columnDefinition = "TEXT")
    private String nota;

    @Column(name = "fecha_nota", nullable = false)
    private Instant fechaNota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesional_id")
    private Personal profesional;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (fechaNota == null) {
            fechaNota = Instant.now();
        }
    }
}
