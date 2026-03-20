/**
 * Token de recuperación de contraseña almacenado en schema {@code public}.
 * Permite validar el token sin JWT (petición anónima) y resolver el tenant asociado al correo.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.entity.master;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens_public", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetTokenPublic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "tenant_schema", nullable = false, length = 63)
    private String tenantSchema;

    @Column(nullable = false, unique = true, length = 128)
    private String token;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(nullable = false)
    @Builder.Default
    private Boolean usado = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
