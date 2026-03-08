/**
 * Plantilla SOAP para evolución en historia clínica (Res. 1995/1999).
 * Catálogo por motivo de consulta para contenido mínimo y homogeneidad.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "plantillas_soap")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlantillaSoap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    /** Motivo de consulta asociado: CONTROL, PRIMERA_VEZ, SEGUIMIENTO_AGUDO, SEGUIMIENTO_CRONICO, VALORACION, OTRO */
    @Column(name = "motivo_tipo", length = 50)
    private String motivoTipo;

    @Column(name = "contenido_subjetivo", columnDefinition = "TEXT")
    private String contenidoSubjetivo;

    @Column(name = "contenido_objetivo", columnDefinition = "TEXT")
    private String contenidoObjetivo;

    @Column(name = "contenido_analisis", columnDefinition = "TEXT")
    private String contenidoAnalisis;

    @Column(name = "contenido_plan", columnDefinition = "TEXT")
    private String contenidoPlan;

    @Column(name = "codigo_cie10_sugerido", length = 20)
    private String codigoCie10Sugerido;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
