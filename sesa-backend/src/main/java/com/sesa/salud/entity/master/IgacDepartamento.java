/**
 * Catálogo IGAC – Departamentos (límites oficiales DANE/IGAC).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity.master;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "igac_departamentos", schema = "public", uniqueConstraints = {
    @UniqueConstraint(columnNames = "codigo_dane")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IgacDepartamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código DANE 2 dígitos (ej. 05 = Antioquia, 11 = Bogotá). */
    @Column(name = "codigo_dane", nullable = false, length = 2)
    private String codigoDane;

    @Column(nullable = false, length = 120)
    private String nombre;
}
