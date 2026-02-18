/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Registro de deduplicación: almacena clientIds ya procesados para evitar
 * que una operación offline se aplique dos veces (idempotencia).
 */
@Entity
@Table(name = "sync_deduplication")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncDeduplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false, unique = true, length = 64)
    private String clientId;

    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "server_id")
    private Long serverId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean success = true;

    @Column(name = "processed_at", nullable = false)
    @Builder.Default
    private Instant processedAt = Instant.now();
}
