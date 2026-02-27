/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "consentimientos_informados")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConsentimientoInformado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Personal profesional;

    /** GENERAL / QUIRURGICO / DIAGNOSTICO / ODONTOLOGICO / ANESTESIA */
    @Column(nullable = false, length = 30)
    private String tipo;

    /** PENDIENTE / FIRMADO / RECHAZADO / REVOCADO */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "PENDIENTE";

    @Column(length = 300)
    private String procedimiento;

    @Column(name = "fecha_solicitud", nullable = false, updatable = false)
    private Instant fechaSolicitud;

    @Column(name = "fecha_firma")
    private Instant fechaFirma;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "firma_canvas_data", columnDefinition = "TEXT")
    private String firmaCanvasData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        fechaSolicitud = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
