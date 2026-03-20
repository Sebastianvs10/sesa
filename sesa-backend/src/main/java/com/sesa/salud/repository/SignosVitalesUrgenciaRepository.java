/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.SignosVitalesUrgencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SignosVitalesUrgenciaRepository extends JpaRepository<SignosVitalesUrgencia, Long> {

    List<SignosVitalesUrgencia> findByUrgenciaRegistroIdOrderByFechaHoraAsc(Long urgenciaRegistroId);
}
