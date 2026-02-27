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
}
