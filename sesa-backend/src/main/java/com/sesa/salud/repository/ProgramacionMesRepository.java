/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.ProgramacionMes;
import com.sesa.salud.entity.enums.EstadoProgramacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProgramacionMesRepository extends JpaRepository<ProgramacionMes, Long> {

    Optional<ProgramacionMes> findByAnioAndMes(Integer anio, Integer mes);

    boolean existsByAnioAndMes(Integer anio, Integer mes);

    boolean existsByAnioAndMesAndEstado(Integer anio, Integer mes, EstadoProgramacion estado);
}
