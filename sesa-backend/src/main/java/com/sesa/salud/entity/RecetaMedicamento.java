/**
 * Item de medicamento en una receta electrónica.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "receta_medicamentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecetaMedicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_id", nullable = false)
    private RecetaElectronica receta;

    @Column(nullable = false, length = 200)
    private String medicamento;

    @Column(length = 100)
    private String dosis;

    @Column(length = 100)
    private String frecuencia;

    @Column(length = 100)
    private String duracion;
}
