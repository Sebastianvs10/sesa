/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.Factura;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface FacturaRepository extends JpaRepository<Factura, Long> {
    List<Factura> findByPaciente_IdOrderByFechaFacturaDesc(Long pacienteId, Pageable pageable);
    List<Factura> findByFechaFacturaBetween(Instant desde, Instant hasta);
}
