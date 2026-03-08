/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Signos vitales seriados por urgencia (sugerencia 5).
 * Permite registrar múltiples tomas durante la espera/atención.
 */
@Entity
@Table(name = "signos_vitales_urgencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignosVitalesUrgencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "urgencia_registro_id", nullable = false)
    private UrgenciaRegistro urgenciaRegistro;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "presion_arterial", length = 20)
    private String presionArterial;

    @Column(name = "frecuencia_cardiaca", length = 10)
    private String frecuenciaCardiaca;

    @Column(name = "frecuencia_respiratoria", length = 10)
    private String frecuenciaRespiratoria;

    @Column(name = "temperatura", length = 10)
    private String temperatura;

    @Column(name = "saturacion_o2", length = 10)
    private String saturacionO2;

    @Column(name = "peso", length = 10)
    private String peso;

    @Column(name = "dolor_eva", length = 5)
    private String dolorEva;

    @Column(name = "glasgow_ocular")
    private Integer glasgowOcular;

    @Column(name = "glasgow_verbal")
    private Integer glasgowVerbal;

    @Column(name = "glasgow_motor")
    private Integer glasgowMotor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (fechaHora == null) fechaHora = LocalDateTime.now();
    }
}
