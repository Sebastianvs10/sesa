/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.Consulta;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    List<Consulta> findByPaciente_IdOrderByFechaConsultaDesc(Long pacienteId, Pageable pageable);

    boolean existsByPaciente_Id(Long pacienteId);

    List<Consulta> findByProfesional_IdOrderByFechaConsultaDesc(Long profesionalId, Pageable pageable);

    List<Consulta> findByFechaConsultaBetween(Instant desde, Instant hasta);
}
