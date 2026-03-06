/**
 * Catálogo IGAC – Veredas (límites oficiales DANE/IGAC).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity.master;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "igac_veredas", schema = "public", indexes = {
    @Index(columnList = "municipio_codigo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IgacVereda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código único vereda (DIVIPOLA o código interno). */
    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(name = "municipio_codigo", nullable = false, length = 5)
    private String municipioCodigo;

    @Column(nullable = false, length = 200)
    private String nombre;

    /** GeoJSON simplificado (opcional); se puede servir desde archivo estático. */
    @Column(columnDefinition = "TEXT")
    private String geometryJson;
}
