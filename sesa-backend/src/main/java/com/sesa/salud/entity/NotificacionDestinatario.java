/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notificacion_destinatarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionDestinatario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notificacion_id", nullable = false)
    private Notificacion notificacion;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "usuario_email", length = 255)
    private String usuarioEmail;

    @Column(name = "usuario_nombre", length = 200)
    private String usuarioNombre;

    @Column(nullable = false)
    @Builder.Default
    private Boolean leido = false;

    @Column(name = "fecha_lectura")
    private Instant fechaLectura;

    @Column(nullable = false)
    @Builder.Default
    private Boolean archivado = false;

    @Column(name = "fecha_archivado")
    private Instant fechaArchivado;

    @Column(nullable = false)
    @Builder.Default
    private Boolean eliminado = false;

    @Column(name = "fecha_eliminado")
    private Instant fechaEliminado;
}
