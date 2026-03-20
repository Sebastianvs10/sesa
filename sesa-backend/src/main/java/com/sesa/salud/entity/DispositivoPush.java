/**
 * Token de dispositivo para notificaciones push (FCM / Web Push).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "dispositivos_push", indexes = {
    @Index(name = "idx_dp_usuario", columnList = "usuario_id"),
    @Index(name = "idx_dp_token", columnList = "token")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispositivoPush {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 512)
    private String token;

    /** WEB, ANDROID, IOS */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String plataforma = "WEB";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
