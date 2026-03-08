/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.UrgenciaRegistro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface UrgenciaRegistroRepository extends JpaRepository<UrgenciaRegistro, Long> {

    Page<UrgenciaRegistro> findByEstado(String estado, Pageable pageable);

    List<UrgenciaRegistro> findByEstadoIn(List<String> estados, Pageable pageable);

    long countByEstado(String estado);

    long countByNivelTriage(String nivelTriage);

    List<UrgenciaRegistro> findByEstadoOrderByFechaHoraIngresoAsc(String estado, Pageable pageable);

    /** Ingresos en rango de fechas (inclusive) para reporte de cumplimiento. */
    List<UrgenciaRegistro> findByFechaHoraIngresoBetweenOrderByFechaHoraIngresoAsc(
            LocalDateTime desde, LocalDateTime hasta);
}
