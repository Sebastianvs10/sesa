/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "consultas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_id")
    private Personal profesional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id")
    private Cita cita;

    @Column(name = "motivo_consulta", columnDefinition = "TEXT")
    private String motivoConsulta;

    @Column(name = "enfermedad_actual", columnDefinition = "TEXT")
    private String enfermedadActual;

    @Column(name = "antecedentes_personales", columnDefinition = "TEXT")
    private String antecedentesPersonales;

    @Column(name = "antecedentes_familiares", columnDefinition = "TEXT")
    private String antecedentesFamiliares;

    @Column(columnDefinition = "TEXT")
    private String alergias;

    @Column(name = "fecha_consulta", nullable = false)
    private Instant fechaConsulta;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Campos normativos Res. 1995/1999 y RIPS
    @Column(name = "tipo_consulta", length = 30)
    private String tipoConsulta;

    @Column(name = "codigo_cie10", length = 20)
    private String codigoCie10;

    @Column(name = "codigo_cie10_secundario", columnDefinition = "TEXT")
    private String codigoCie10Secundario;

    @Column(name = "dolor_eva", length = 5)
    private String dolorEva;

    @Column(name = "perimetro_abdominal", length = 10)
    private String perimetroAbdominal;

    @Column(name = "perimetro_cefalico", length = 10)
    private String perimetroCefalico;

    @Column(name = "saturacion_o2", length = 10)
    private String saturacionO2;

    @Column(name = "presion_arterial", length = 20)
    private String presionArterial;

    @Column(name = "frecuencia_cardiaca", length = 10)
    private String frecuenciaCardiaca;

    @Column(name = "frecuencia_respiratoria", length = 10)
    private String frecuenciaRespiratoria;

    @Column(length = 10)
    private String temperatura;

    @Column(length = 10)
    private String peso;

    @Column(length = 10)
    private String talla;

    @Column(length = 10)
    private String imc;

    @Column(name = "hallazgos_examen", columnDefinition = "TEXT")
    private String hallazgosExamen;

    @Column(columnDefinition = "TEXT")
    private String diagnostico;

    @Column(name = "plan_tratamiento", columnDefinition = "TEXT")
    private String planTratamiento;

    @Column(name = "tratamiento_farmacologico", columnDefinition = "TEXT")
    private String tratamientoFarmacologico;

    @Column(name = "observaciones_clinicas", columnDefinition = "TEXT")
    private String observacionesClincias;

    @Column(columnDefinition = "TEXT")
    private String recomendaciones;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (fechaConsulta == null) {
            fechaConsulta = Instant.now();
        }
    }
}
