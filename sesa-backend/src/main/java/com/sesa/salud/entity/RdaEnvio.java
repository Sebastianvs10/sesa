/**
 * Entidad de trazabilidad de envíos RDA al Ministerio de Salud
 * Resolución 1888 de 2025 — Interoperabilidad IHCE Colombia
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "rda_envios", indexes = {
        @Index(name = "idx_rda_atencion", columnList = "atencion_id"),
        @Index(name = "idx_rda_estado",   columnList = "estado_envio")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RdaEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tipo de RDA según Resolución 1888/2025 */
    @Column(name = "tipo_rda", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private TipoRda tipoRda;

    /** Estado del ciclo de vida del envío */
    @Column(name = "estado_envio", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EstadoRda estadoEnvio;

    @Column(name = "atencion_id")
    private Long atencionId;

    /** Bundle FHIR R4 generado (JSON) */
    @Column(name = "bundle_json", columnDefinition = "TEXT")
    private String bundleJson;

    /** ID asignado por la plataforma IHCE del Ministerio */
    @Column(name = "id_ministerio", length = 100)
    private String idMinisterio;

    /** Versión del bundle FHIR */
    @Column(name = "fhir_version", length = 10)
    @Builder.Default
    private String fhirVersion = "4.0.1";

    @Column(name = "fecha_generacion", nullable = false)
    private Instant fechaGeneracion;

    @Column(name = "fecha_envio")
    private Instant fechaEnvio;

    @Column(name = "fecha_confirmacion")
    private Instant fechaConfirmacion;

    @Column(name = "error_mensaje", columnDefinition = "TEXT")
    private String errorMensaje;

    @Column(name = "reintentos")
    @Builder.Default
    private Integer reintentos = 0;

    /** Schema del tenant (multi-tenancy) */
    @Column(name = "tenant_schema", nullable = false, length = 100)
    private String tenantSchema;

    @PrePersist
    protected void onCreate() {
        if (fechaGeneracion == null) fechaGeneracion = Instant.now();
        if (estadoEnvio == null) estadoEnvio = EstadoRda.PENDIENTE;
    }

    // ─── Enums ──────────────────────────────────────────────────────────────

    public enum TipoRda {
        /** Generado al finalizar una atención ambulatoria */
        CONSULTA_EXTERNA,
        /** Consolidado al alta hospitalaria */
        HOSPITALIZACION,
        /** Creado tras atención en urgencias */
        URGENCIAS,
        /** Generado tras cualquier evento clínico */
        PACIENTE
    }

    public enum EstadoRda {
        /** Bundle generado, pendiente de envío */
        PENDIENTE,
        /** Enviado a la plataforma IHCE del Ministerio */
        ENVIADO,
        /** Confirmado y aceptado por el Ministerio */
        CONFIRMADO,
        /** Error en la transmisión o validación */
        ERROR
    }
}
