/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.LaboratorioSolicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LaboratorioSolicitudRepository extends JpaRepository<LaboratorioSolicitud, Long> {

    Page<LaboratorioSolicitud> findByPaciente_Id(Long pacienteId, Pageable pageable);

    List<LaboratorioSolicitud> findByEstado(String estado, Pageable pageable);
}
