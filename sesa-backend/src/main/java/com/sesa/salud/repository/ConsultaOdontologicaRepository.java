/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.ConsultaOdontologica;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConsultaOdontologicaRepository extends JpaRepository<ConsultaOdontologica, Long> {

    Page<ConsultaOdontologica> findByPaciente_IdOrderByCreatedAtDesc(Long pacienteId, Pageable pageable);

    List<ConsultaOdontologica> findByPaciente_IdOrderByCreatedAtDesc(Long pacienteId);

    @Query("SELECT c FROM ConsultaOdontologica c WHERE c.profesional.id = :profesionalId ORDER BY c.createdAt DESC")
    Page<ConsultaOdontologica> findByProfesionalId(@Param("profesionalId") Long profesionalId, Pageable pageable);
}
