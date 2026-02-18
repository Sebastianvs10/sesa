/**
 * Entidad Auditoría Historia Clínica - Registro legal inmodificable
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "auditoria_historia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaHistoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tabla_afectada", nullable = false, length = 100)
    private String tablaAfectada;

    @Column(name = "registro_id")
    private Long registroId;

    @Column(nullable = false, length = 20)
    private String accion;

    @Column(length = 255)
    private String usuario;

    @Column(nullable = false)
    private Instant fecha;

    @Column(length = 45)
    private String ip;

    @Column(name = "valor_antes", columnDefinition = "TEXT")
    private String valorAntes;

    @Column(name = "valor_despues", columnDefinition = "TEXT")
    private String valorDespues;

    @PrePersist
    protected void onCreate() {
        if (fecha == null) {
            fecha = Instant.now();
        }
    }
}
