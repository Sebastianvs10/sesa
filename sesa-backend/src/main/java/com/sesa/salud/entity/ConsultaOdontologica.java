/**
 * Consulta odontológica — historia clínica dental por visita.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "consultas_odontologicas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConsultaOdontologica {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Personal profesional;

    /** Cita asociada (opcional) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id")
    private Cita cita;

    // ── Tipo de consulta (Res. 1995/1999) ─────────────────────────────
    /** PRIMERA_VEZ / CONTROL / URGENCIA_ODONTOLOGICA / INTERCONSULTA */
    @Column(name = "tipo_consulta", length = 40)
    private String tipoConsulta;

    // ── CIE-10 K00-K14 ────────────────────────────────────────────────
    @Column(name = "codigo_cie10", length = 10)
    private String codigoCie10;

    @Column(name = "descripcion_cie10", length = 200)
    private String descripcionCie10;

    // ── Consentimiento informado odontológico ─────────────────────────
    @Column(name = "consentimiento_firmado")
    private Boolean consentimientoFirmado;

    @Column(name = "fecha_consentimiento")
    private LocalDate fechaConsentimiento;

    @Column(name = "consentimiento_observaciones", columnDefinition = "TEXT")
    private String consentimientoObservaciones;

    // ── SOAP Subjetivo ─────────────────────────────────────────────────
    @Column(name = "motivo_consulta", columnDefinition = "TEXT")
    private String motivoConsulta;

    @Column(name = "enfermedad_actual", columnDefinition = "TEXT")
    private String enfermedadActual;

    @Column(name = "antecedentes_odontologicos", columnDefinition = "TEXT")
    private String antecedentesOdontologicos;

    @Column(name = "antecedentes_sistemicos", columnDefinition = "TEXT")
    private String antecedentesSistemicos;

    @Column(name = "medicamentos_actuales", columnDefinition = "TEXT")
    private String medicamentosActuales;

    @Column(length = 500)
    private String alergias;

    @Column(name = "habitos_orales", columnDefinition = "TEXT")
    private String habitosOrales;

    /** BUENA / REGULAR / MALA */
    @Column(name = "higiene_oral", length = 30)
    private String higieneOral;

    // ── SOAP Objetivo ──────────────────────────────────────────────────
    @Column(name = "examen_extra_oral", columnDefinition = "TEXT")
    private String examenExtraOral;

    @Column(name = "examen_intra_oral", columnDefinition = "TEXT")
    private String examenIntraOral;

    /** Índice CPOD (dentición permanente) */
    @Column(name = "cpod_cariados")
    private Integer cpodCariados;

    @Column(name = "cpod_perdidos")
    private Integer cpodPerdidos;

    @Column(name = "cpod_obturados")
    private Integer cpodObturados;

    /** Índice ceod (dentición temporal) */
    @Column(name = "ceod_cariados")
    private Integer ceodCariados;

    @Column(name = "ceod_extraidos")
    private Integer ceodExtraidos;

    @Column(name = "ceod_obturados")
    private Integer ceodObturados;

    /** Índice IHO-S (Higiene Oral Simplificado) */
    @Column(name = "ihos_placa")
    private Double ihosPlaca;

    @Column(name = "ihos_calculo")
    private Double ihosCalculo;

    /** LEVE / MODERADA / SEVERA / SANA */
    @Column(name = "condicion_periodontal", length = 30)
    private String condicionPeriodontal;

    /** BAJO / MEDIO / ALTO */
    @Column(name = "riesgo_caries", length = 20)
    private String riesgoCaries;

    // ── SOAP Análisis / Plan ───────────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String diagnostico;

    @Column(name = "plan_tratamiento", columnDefinition = "TEXT")
    private String planTratamiento;

    // ── Firma digital ──────────────────────────────────────────────────
    @Column(name = "firma_profesional_url", columnDefinition = "TEXT")
    private String firmaProfesionalUrl;

    @Column(name = "firma_canvas_data", columnDefinition = "TEXT")
    private String firmaCanvasData;

    /** EN_ATENCION / FINALIZADO / CANCELADO */
    @Column(length = 30)
    @Builder.Default
    private String estado = "EN_ATENCION";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = Instant.now(); updatedAt = Instant.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = Instant.now(); }
}
