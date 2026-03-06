/**
 * Catálogo IGAC – Municipios (límites oficiales DANE/IGAC).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity.master;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "igac_municipios", schema = "public", uniqueConstraints = {
    @UniqueConstraint(columnNames = "codigo_dane")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IgacMunicipio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código DANE 5 dígitos (ej. 05001 = Medellín). */
    @Column(name = "codigo_dane", nullable = false, length = 5)
    private String codigoDane;

    @Column(name = "departamento_codigo", nullable = false, length = 2)
    private String departamentoCodigo;

    @Column(nullable = false, length = 120)
    private String nombre;
}
