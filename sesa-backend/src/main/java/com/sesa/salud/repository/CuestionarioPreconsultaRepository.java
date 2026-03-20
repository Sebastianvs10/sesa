/**
 * S10: Cuestionarios pre-consulta (ePRO).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.CuestionarioPreconsulta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CuestionarioPreconsultaRepository extends JpaRepository<CuestionarioPreconsulta, Long> {

    Optional<CuestionarioPreconsulta> findByCitaId(Long citaId);

    List<CuestionarioPreconsulta> findByPacienteIdOrderByEnviadoAtDesc(Long pacienteId);
}
