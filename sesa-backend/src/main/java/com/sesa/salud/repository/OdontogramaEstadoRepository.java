/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.OdontogramaEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OdontogramaEstadoRepository extends JpaRepository<OdontogramaEstado, Long> {

    /** Estado actual del odontograma (último registro por pieza+superficie). */
    @Query("""
        SELECT o FROM OdontogramaEstado o
        WHERE o.paciente.id = :pacienteId
          AND o.id IN (
            SELECT MAX(o2.id) FROM OdontogramaEstado o2
            WHERE o2.paciente.id = :pacienteId
            GROUP BY o2.piezaFdi, o2.superficie
          )
        ORDER BY o.piezaFdi, o.superficie
        """)
    List<OdontogramaEstado> findEstadoActualByPacienteId(@Param("pacienteId") Long pacienteId);

    /** Historial completo de cambios de una pieza. */
    List<OdontogramaEstado> findByPaciente_IdAndPiezaFdiOrderByCreatedAtDesc(Long pacienteId, Integer piezaFdi);

    /** Todos los cambios registrados en una consulta específica. */
    List<OdontogramaEstado> findByConsulta_IdOrderByPiezaFdiAsc(Long consultaId);
}
