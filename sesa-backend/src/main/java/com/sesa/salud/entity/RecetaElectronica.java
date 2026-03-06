/**
 * Receta electrónica con token verificable (anti-falsificación).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "recetas_electronicas", indexes = @Index(columnList = "token_verificacion", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecetaElectronica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_verificacion", nullable = false, unique = true, length = 64)
    private String tokenVerificacion;

    @Column(name = "atencion_id")
    private Long atencionId;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "consulta_id")
    private Long consultaId;

    @Column(name = "medico_nombre", nullable = false, length = 200)
    private String medicoNombre;

    @Column(name = "medico_tarjeta_profesional", length = 50)
    private String medicoTarjetaProfesional;

    @Column(name = "paciente_nombre", nullable = false, length = 200)
    private String pacienteNombre;

    @Column(name = "paciente_documento", length = 50)
    private String pacienteDocumento;

    @Column(name = "fecha_emision", nullable = false)
    private Instant fechaEmision;

    @Column(columnDefinition = "TEXT")
    private String diagnostico;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "valida_hasta")
    private Instant validaHasta;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RecetaMedicamento> medicamentos = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (tokenVerificacion == null || tokenVerificacion.isBlank()) {
            tokenVerificacion = UUID.randomUUID().toString().replace("-", "");
        }
        if (fechaEmision == null) {
            fechaEmision = Instant.now();
        }
        if (validaHasta == null) {
            validaHasta = fechaEmision.plus(90, ChronoUnit.DAYS);
        }
    }
}
