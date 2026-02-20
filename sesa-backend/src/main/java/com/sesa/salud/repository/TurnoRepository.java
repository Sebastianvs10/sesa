/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.Turno;
import com.sesa.salud.entity.enums.ServicioClinico;
import com.sesa.salud.entity.enums.TipoTurno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TurnoRepository extends JpaRepository<Turno, Long> {

    /** Todos los turnos de un mes (para la vista de calendario). */
    @Query("""
        SELECT t FROM Turno t
        JOIN FETCH t.personal p
        WHERE t.programacionMes.id = :programacionMesId
        ORDER BY t.fechaInicio
    """)
    List<Turno> findByProgramacionMesId(@Param("programacionMesId") Long programacionMesId);

    /** Turnos de un profesional en un mes (para validaciones de carga). */
    @Query("""
        SELECT t FROM Turno t
        WHERE t.personal.id = :personalId
          AND t.programacionMes.id = :programacionMesId
        ORDER BY t.fechaInicio
    """)
    List<Turno> findByPersonalIdAndProgramacionMesId(
            @Param("personalId") Long personalId,
            @Param("programacionMesId") Long programacionMesId);

    /**
     * Detecta solapamiento de turnos para el mismo profesional.
     * Un solapamiento ocurre cuando el nuevo turno comienza antes de que termine otro
     * y termina después de que comienza ese otro.
     */
    @Query("""
        SELECT t FROM Turno t
        WHERE t.personal.id   = :personalId
          AND t.id            <> :excludeId
          AND t.fechaInicio    < :fechaFin
          AND t.fechaFin       > :fechaInicio
    """)
    List<Turno> findSolapados(
            @Param("personalId")  Long personalId,
            @Param("excludeId")   Long excludeId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin")    LocalDateTime fechaFin);

    /**
     * Turnos del profesional en un rango de fechas (para validar horas semanales).
     */
    @Query("""
        SELECT t FROM Turno t
        WHERE t.personal.id   = :personalId
          AND t.id            <> :excludeId
          AND t.fechaInicio   >= :desde
          AND t.fechaInicio    < :hasta
    """)
    List<Turno> findByPersonalEnRango(
            @Param("personalId") Long personalId,
            @Param("excludeId")  Long excludeId,
            @Param("desde")      LocalDateTime desde,
            @Param("hasta")      LocalDateTime hasta);

    /** Filtros opcionales sobre la lista del mes. */
    @Query("""
        SELECT t FROM Turno t
        JOIN FETCH t.personal p
        WHERE t.programacionMes.id = :programacionMesId
          AND (:servicio  IS NULL OR t.servicio  = :servicio)
          AND (:tipoTurno IS NULL OR t.tipoTurno = :tipoTurno)
          AND (:personalId IS NULL OR p.id       = :personalId)
        ORDER BY t.fechaInicio
    """)
    List<Turno> findByProgramacionMesConFiltros(
            @Param("programacionMesId") Long programacionMesId,
            @Param("servicio")    ServicioClinico servicio,
            @Param("tipoTurno")   TipoTurno tipoTurno,
            @Param("personalId")  Long personalId);
}
