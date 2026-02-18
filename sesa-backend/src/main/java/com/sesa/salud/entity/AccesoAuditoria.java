/**
 * Auditoría de accesos de autenticación
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "acceso_auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccesoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 40)
    private String evento;

    @Column(length = 45)
    private String ip;

    @Column(columnDefinition = "TEXT")
    private String detalle;

    @Column(nullable = false)
    private Instant fecha;

    @PrePersist
    protected void onCreate() {
        if (fecha == null) fecha = Instant.now();
    }
}
