/**
 * Imagen clínica o radiografía asociada a un paciente/consulta/pieza dental.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "imagenes_clinicas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImagenClinica {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Personal profesional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id")
    private ConsultaOdontologica consulta;

    /** Pieza FDI asociada (opcional) */
    @Column(name = "pieza_fdi")
    private Integer piezaFdi;

    /** RADIOGRAFIA_PERIAPICAL | RADIOGRAFIA_PANORAMICA | FOTO_CLINICA | MODELO | OTRO */
    @Column(length = 50)
    @Builder.Default
    private String tipo = "FOTO_CLINICA";

    @Column(name = "nombre_archivo", length = 300)
    private String nombreArchivo;

    /** URL pública o ruta en servidor */
    @Column(columnDefinition = "TEXT")
    private String url;

    /** Base64 para imágenes pequeñas o thumbnails */
    @Column(name = "thumbnail_base64", columnDefinition = "TEXT")
    private String thumbnailBase64;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() { createdAt = Instant.now(); }
}
