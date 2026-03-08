/**
 * Ítem (línea) de una orden clínica. Una orden puede tener varios ítems.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "orden_clinica_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenClinicaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenClinica orden;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(columnDefinition = "TEXT")
    private String detalle;

    @Column(name = "cantidad_prescrita")
    private Integer cantidadPrescrita;

    @Column(name = "unidad_medida", length = 30)
    private String unidadMedida;

    @Column(length = 120)
    private String frecuencia;

    @Column(name = "duracion_dias")
    private Integer duracionDias;

    @Column(name = "valor_estimado", precision = 14, scale = 2)
    private BigDecimal valorEstimado;

    @Column(name = "orden_item_index", nullable = false)
    @Builder.Default
    private Integer ordenItemIndex = 0;
}
