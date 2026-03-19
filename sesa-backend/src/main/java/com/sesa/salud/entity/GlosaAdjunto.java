/**
 * S9: Adjunto de glosa — documento asociado a una glosa.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "glosa_adjuntos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlosaAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "glosa_id", nullable = false)
    private Glosa glosa;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(length = 50)
    private String tipo;

    @Column(name = "url_o_blob", columnDefinition = "TEXT")
    private String urlOBlob;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
