/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.OrdenClinica;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdenClinicaRepository extends JpaRepository<OrdenClinica, Long> {
    List<OrdenClinica> findByPaciente_IdOrderByCreatedAtDesc(Long pacienteId, Pageable pageable);
}
