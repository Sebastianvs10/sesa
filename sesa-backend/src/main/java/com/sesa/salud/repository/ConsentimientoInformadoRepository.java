/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.ConsentimientoInformado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsentimientoInformadoRepository extends JpaRepository<ConsentimientoInformado, Long> {

    List<ConsentimientoInformado> findByPaciente_IdOrderByCreatedAtDesc(Long pacienteId);

    List<ConsentimientoInformado> findByPaciente_IdAndEstadoOrderByCreatedAtDesc(Long pacienteId, String estado);
}
