/**
 * Entidad Atención / Consulta - Cada encuentro clínico del paciente
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "atenciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Atencion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "historia_id", nullable = false)
    private HistoriaClinica historiaClinica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Personal profesional;

    @Column(name = "fecha_atencion", nullable = false)
    private Instant fechaAtencion;

    @Column(name = "motivo_consulta", columnDefinition = "TEXT")
    private String motivoConsulta;

    @Column(name = "enfermedad_actual", columnDefinition = "TEXT")
    private String enfermedadActual;

    @Column(name = "version_enfermedad", columnDefinition = "TEXT")
    private String versionEnfermedad;

    @Column(name = "sintomas_asociados", columnDefinition = "TEXT")
    private String sintomasAsociados;

    @Column(name = "factores_mejoran", columnDefinition = "TEXT")
    private String factoresMejoran;

    @Column(name = "factores_empeoran", columnDefinition = "TEXT")
    private String factoresEmpeoran;

    @Column(name = "revision_sistemas", columnDefinition = "TEXT")
    private String revisionSistemas;

    @Column(name = "presion_arterial", length = 20)
    private String presionArterial;

    @Column(name = "frecuencia_cardiaca", length = 10)
    private String frecuenciaCardiaca;

    @Column(name = "frecuencia_respiratoria", length = 10)
    private String frecuenciaRespiratoria;

    @Column(name = "temperatura", length = 10)
    private String temperatura;

    @Column(name = "peso", length = 10)
    private String peso;

    @Column(name = "talla", length = 10)
    private String talla;

    @Column(name = "imc", length = 10)
    private String imc;

    @Column(name = "evaluacion_general", columnDefinition = "TEXT")
    private String evaluacionGeneral;

    @Column(name = "hallazgos", columnDefinition = "TEXT")
    private String hallazgos;

    @Column(name = "diagnostico", columnDefinition = "TEXT")
    private String diagnostico;

    @Column(name = "codigo_cie10", length = 20)
    private String codigoCie10;

    @Column(name = "plan_tratamiento", columnDefinition = "TEXT")
    private String planTratamiento;

    @Column(name = "tratamiento_farmacologico", columnDefinition = "TEXT")
    private String tratamientoFarmacologico;

    @Column(name = "ordenes_medicas", columnDefinition = "TEXT")
    private String ordenesMedicas;

    @Column(name = "examenes_solicitados", columnDefinition = "TEXT")
    private String examenesSolicitados;

    @Column(name = "incapacidad", columnDefinition = "TEXT")
    private String incapacidad;

    @Column(name = "recomendaciones", columnDefinition = "TEXT")
    private String recomendaciones;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "atencion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Diagnostico> diagnosticos = new ArrayList<>();

    @OneToMany(mappedBy = "atencion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Procedimiento> procedimientos = new ArrayList<>();

    @OneToMany(mappedBy = "atencion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FormulaMedica> formulasMedicas = new ArrayList<>();

    @OneToMany(mappedBy = "atencion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LaboratorioAtencion> laboratorios = new ArrayList<>();

    @OneToMany(mappedBy = "atencion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ImagenDiagnostica> imagenesDiagnosticas = new ArrayList<>();

    @OneToMany(mappedBy = "atencion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Evolucion> evoluciones = new ArrayList<>();

    @OneToMany(mappedBy = "atencion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NotaEnfermeria> notasEnfermeria = new ArrayList<>();

    @OneToMany(mappedBy = "atencion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Consentimiento> consentimientos = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (fechaAtencion == null) {
            fechaAtencion = Instant.now();
        }
    }
}
