/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notificaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(length = 30)
    @Builder.Default
    private String tipo = "GENERAL";

    @Column(name = "remitente_id", nullable = false)
    private Long remitenteId;

    @Column(name = "remitente_nombre", length = 200)
    private String remitenteNombre;

    @Column(name = "fecha_envio", nullable = false)
    private Instant fechaEnvio;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "cita_id")
    private Long citaId;

    @OneToMany(mappedBy = "notificacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NotificacionAdjunto> adjuntos = new ArrayList<>();

    @OneToMany(mappedBy = "notificacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NotificacionDestinatario> destinatarios = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (fechaEnvio == null) {
            fechaEnvio = Instant.now();
        }
    }
}
