/**
 * S12: Entidad API Key para integradores (laboratorio, PACS, signos vitales).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "api_keys", indexes = {
        @Index(name = "idx_api_keys_index", columnList = "api_key_index", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_integrador", nullable = false, length = 150)
    private String nombreIntegrador;

    @Column(name = "api_key_hash", nullable = false, length = 255)
    private String apiKeyHash;

    /** Índice para búsqueda (SHA-256 del raw key); no se usa la clave en claro. */
    @Column(name = "api_key_index", nullable = false, length = 64)
    private String apiKeyIndex;

    /** Comma-separated: LABORATORIO, IMAGEN, SIGNOS_VITALES */
    @Column(name = "permisos", nullable = false, length = 200)
    @Builder.Default
    private String permisos = "LABORATORIO";

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public boolean hasPermiso(String permiso) {
        if (permisos == null) return false;
        for (String p : permisos.split(",")) {
            if (p.trim().equalsIgnoreCase(permiso)) return true;
        }
        return false;
    }
}
