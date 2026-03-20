/**
 * Ítem o línea de detalle de una factura (cuenta médica multiclínea).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "factura_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;

    @Column(name = "item_index", nullable = false)
    @Builder.Default
    private Integer itemIndex = 0;

    @Column(name = "codigo_cups", length = 20)
    private String codigoCups;

    @Column(name = "descripcion_cups", length = 500)
    private String descripcionCups;

    @Column(name = "tipo_servicio", length = 40)
    private String tipoServicio;

    @Column(nullable = false)
    @Builder.Default
    private Integer cantidad = 1;

    @Column(name = "valor_unitario", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorUnitario;

    @Column(name = "valor_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorTotal;

    /** Opcional: trazabilidad a orden clínica ítem. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_clinica_item_id")
    private OrdenClinicaItem ordenClinicaItem;
}
