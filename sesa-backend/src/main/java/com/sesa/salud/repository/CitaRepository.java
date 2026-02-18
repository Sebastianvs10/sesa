/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.Cita;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    Page<Cita> findByPaciente_Id(Long pacienteId, Pageable pageable);

    @Query("SELECT c FROM Cita c WHERE DATE(c.fechaHora) = :fecha ORDER BY c.fechaHora")
    List<Cita> findByFecha(LocalDate fecha);

    List<Cita> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin);

    long countByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT c.estado, COUNT(c) FROM Cita c GROUP BY c.estado")
    List<Object[]> countGroupByEstado();
}
