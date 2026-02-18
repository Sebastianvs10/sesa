/**
 * Entidad Consentimiento Informado - Con evidencia de firma
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "consentimientos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consentimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atencion_id", nullable = false)
    private Atencion atencion;

    @Column(nullable = false, length = 200)
    private String tipo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "firma_digital", columnDefinition = "TEXT")
    private String firmaDigital;

    @Column(name = "evidencia_url", length = 500)
    private String evidenciaUrl;

    @Column(name = "fecha_consentimiento", nullable = false)
    private Instant fechaConsentimiento;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (fechaConsentimiento == null) {
            fechaConsentimiento = Instant.now();
        }
    }
}
