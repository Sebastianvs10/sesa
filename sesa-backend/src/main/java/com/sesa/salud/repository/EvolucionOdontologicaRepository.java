/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.EvolucionOdontologica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvolucionOdontologicaRepository extends JpaRepository<EvolucionOdontologica, Long> {

    List<EvolucionOdontologica> findByPaciente_IdOrderByCreatedAtDesc(Long pacienteId);

    List<EvolucionOdontologica> findByConsulta_IdOrderByCreatedAtDesc(Long consultaId);
}
