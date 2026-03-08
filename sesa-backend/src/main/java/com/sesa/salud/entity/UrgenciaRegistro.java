/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "urgencias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrgenciaRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Column(name = "nivel_triage", length = 50)
    private String nivelTriage;

    @Column(length = 50)
    @Builder.Default
    private String estado = "EN_ESPERA";

    @Column(name = "fecha_hora_ingreso", nullable = false)
    private LocalDateTime fechaHoraIngreso;

    /** Registrada al pasar a EN_ATENCION; usada para reporte de cumplimiento de tiempos (Res. 5596/2015). */
    @Column(name = "fecha_hora_inicio_atencion")
    private LocalDateTime fechaHoraInicioAtencion;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atencion_id")
    private Atencion atencion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Campos normativos Res. 5596/2015 (Triage hospitalario)
    @Column(name = "tipo_llegada", length = 30)
    private String tipoLlegada;

    @Column(name = "motivo_consulta", columnDefinition = "TEXT")
    private String motivoConsulta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesional_triage_id")
    private Personal profesionalTriage;

    // Signos vitales al ingreso (triage)
    @Column(name = "sv_presion_arterial", length = 20)
    private String svPresionArterial;

    @Column(name = "sv_frecuencia_cardiaca", length = 10)
    private String svFrecuenciaCardiaca;

    @Column(name = "sv_frecuencia_respiratoria", length = 10)
    private String svFrecuenciaRespiratoria;

    @Column(name = "sv_temperatura", length = 10)
    private String svTemperatura;

    @Column(name = "sv_saturacion_o2", length = 10)
    private String svSaturacionO2;

    @Column(name = "sv_peso", length = 10)
    private String svPeso;

    @Column(name = "sv_dolor_eva", length = 5)
    private String svDolorEva;

    // Escala de Glasgow
    @Column(name = "glasgow_ocular")
    private Integer glasgowOcular;

    @Column(name = "glasgow_verbal")
    private Integer glasgowVerbal;

    @Column(name = "glasgow_motor")
    private Integer glasgowMotor;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (fechaHoraIngreso == null) {
            fechaHoraIngreso = LocalDateTime.now();
        }
    }
}
