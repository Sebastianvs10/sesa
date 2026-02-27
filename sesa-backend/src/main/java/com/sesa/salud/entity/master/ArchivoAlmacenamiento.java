/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity.master;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Almacena archivos (imágenes, PDFs, etc.) como texto base64 en el schema público (master).
 * El campo {@code uuid} actúa como identificador público y se usa en las URLs:
 * {@code GET /api/archivos/{uuid}}.
 *
 * <p>Dos niveles de acceso:</p>
 * <ul>
 *   <li>{@code acceso_publico = true}  → logos de empresa, sin autenticación requerida.</li>
 *   <li>{@code acceso_publico = false} → recursos clínicos, requieren JWT válido del tenant propietario.</li>
 * </ul>
 */
@Entity
@Table(name = "archivo_almacenamiento", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchivoAlmacenamiento {

    @Id
    @Column(name = "uuid", length = 36, nullable = false, updatable = false)
    private String uuid;

    /** Nombre original del archivo tal como lo subió el usuario. */
    @Column(name = "nombre_original", length = 255)
    private String nombreOriginal;

    /** MIME type del archivo (image/png, image/jpeg, image/webp, image/svg+xml, etc.). */
    @Column(name = "content_type", length = 100, nullable = false)
    private String contentType;

    /**
     * Contenido del archivo codificado en base64 (sin prefijo data:URI).
     * Tipo TEXT en PostgreSQL; sin límite explícito de longitud.
     */
    @Lob
    @Column(name = "datos", nullable = false, columnDefinition = "TEXT")
    private String datos;

    /** Tamaño original en bytes (antes de codificar). */
    @Column(name = "tamanio_bytes")
    private Integer tamanioBytes;

    /**
     * {@code true} → el recurso se sirve sin autenticación (logos de empresa, imágenes de branding).
     * {@code false} → requiere JWT válido y schema coincidente.
     */
    @Column(name = "acceso_publico", nullable = false)
    @Builder.Default
    private boolean accesoPublico = false;

    /**
     * Tenant al que pertenece el recurso.
     * "public" para recursos maestros (logos de empresa).
     * Nombre del schema para recursos clínicos del tenant.
     */
    @Column(name = "schema_tenant", length = 100, nullable = false)
    @Builder.Default
    private String schemaTenant = "public";

    /** Tipo de entidad que referencia este archivo (p. ej. "EMPRESA_LOGO", "PACIENTE_FOTO"). */
    @Column(name = "entidad", length = 100)
    private String entidad;

    /** ID de la entidad que referencia este archivo. */
    @Column(name = "entidad_id", length = 50)
    private String entidadId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
