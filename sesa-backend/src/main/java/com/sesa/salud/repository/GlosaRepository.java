/**
 * S9: Glosas — rechazos de factura.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.Glosa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface GlosaRepository extends JpaRepository<Glosa, Long> {

    List<Glosa> findByFactura_IdOrderByFechaRegistroDesc(Long facturaId);

    List<Glosa> findByEstadoOrderByFechaRegistroDesc(String estado);

    List<Glosa> findByFechaRegistroBetweenOrderByFechaRegistroDesc(Instant desde, Instant hasta);

    long countByEstado(String estado);
}
