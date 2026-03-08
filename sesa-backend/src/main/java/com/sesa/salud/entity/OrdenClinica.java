/**
 * Entidad Orden Clínica - salida de una consulta médica
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordenes_clinicas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenClinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id", nullable = false)
    private Consulta consulta;

    /** Tipo/detalle en cabecera: usado para órdenes legacy de un solo ítem. Si hay ítems en orden_clinica_items, puede ser null o "COMPUESTA". */

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(columnDefinition = "TEXT")
    private String detalle;

    /** Cantidad prescrita (ej. 50 para 50 tabletas). Solo órdenes tipo MEDICAMENTO. */
    @Column(name = "cantidad_prescrita")
    private Integer cantidadPrescrita;

    /** Unidad de medida: TAB, ML, GOTAS, FRASCO, SOBRE, UNIDAD, etc. */
    @Column(name = "unidad_medida", length = 30)
    private String unidadMedida;

    /** Frecuencia de toma: cada 8 horas, cada 12 horas, cada 24 horas, etc. */
    @Column(length = 120)
    private String frecuencia;

    /** Duración del tratamiento en días (ej. 7). */
    @Column(name = "duracion_dias")
    private Integer duracionDias;

    @Column(length = 30)
    @Builder.Default
    private String estado = "PENDIENTE";

    /** Resultado de la orden (ej. resultado de laboratorio) cuando estado = COMPLETADO. */
    @Column(columnDefinition = "TEXT")
    private String resultado;

    @Column(name = "fecha_resultado")
    private Instant fechaResultado;

    /** Profesional que registró el resultado (ej. bacteriólogo). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resultado_registrado_por_id")
    private Personal resultadoRegistradoPor;

    @Column(name = "valor_estimado", precision = 14, scale = 2)
    private BigDecimal valorEstimado;

    /** Estado de dispensación en farmacia: PENDIENTE, PARCIAL, COMPLETADA, CANCELADA. */
    @Column(name = "estado_dispensacion_farmacia", length = 30)
    private String estadoDispensacionFarmacia;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordenItemIndex ASC")
    @Builder.Default
    private List<OrdenClinicaItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
