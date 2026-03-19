/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.Cita;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    Page<Cita> findByPaciente_Id(Long pacienteId, Pageable pageable);

    @Query("SELECT c FROM Cita c WHERE DATE(c.fechaHora) = :fecha ORDER BY c.fechaHora")
    List<Cita> findByFecha(LocalDate fecha);

    @Query("SELECT c FROM Cita c WHERE DATE(c.fechaHora) = :fecha AND c.profesional.id = :profesionalId ORDER BY c.fechaHora")
    List<Cita> findByFechaAndProfesionalId(@Param("fecha") LocalDate fecha, @Param("profesionalId") Long profesionalId);

    List<Cita> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin);

    long countByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT c.estado, COUNT(c) FROM Cita c GROUP BY c.estado")
    List<Object[]> countGroupByEstado();

    @Query("SELECT c.estado, COUNT(c) FROM Cita c WHERE DATE(c.fechaHora) = :fecha AND c.profesional.id = :profesionalId GROUP BY c.estado")
    List<Object[]> countByEstadoAndFechaAndProfesionalId(@Param("fecha") LocalDate fecha, @Param("profesionalId") Long profesionalId);

    @Query("SELECT c.estado, COUNT(c) FROM Cita c WHERE DATE(c.fechaHora) = :fecha GROUP BY c.estado")
    List<Object[]> countByEstadoAndFecha(@Param("fecha") LocalDate fecha);

    /** Citas en ventana 24h (recordatorio): entre inicio y fin, estado AGENDADA, sin recordatorio 24h enviado. */
    @Query("SELECT c FROM Cita c WHERE c.fechaHora BETWEEN :inicio AND :fin AND c.estado = 'AGENDADA' AND c.recordatorio24hEnviadoAt IS NULL")
    List<Cita> findParaRecordatorio24h(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    /** Citas en ventana 1h (recordatorio): entre inicio y fin, estado AGENDADA, sin recordatorio 1h enviado. */
    @Query("SELECT c FROM Cita c WHERE c.fechaHora BETWEEN :inicio AND :fin AND c.estado = 'AGENDADA' AND c.recordatorio1hEnviadoAt IS NULL")
    List<Cita> findParaRecordatorio1h(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    /** S3: Buscar cita por token de confirmación (enlace público). */
    java.util.Optional<Cita> findByTokenConfirmacion(String tokenConfirmacion);
}
