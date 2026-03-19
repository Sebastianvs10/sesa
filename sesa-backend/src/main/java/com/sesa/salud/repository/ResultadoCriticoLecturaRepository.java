/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.ResultadoCriticoLectura;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultadoCriticoLecturaRepository extends JpaRepository<ResultadoCriticoLectura, Long> {

    boolean existsByOrdenClinica_IdAndPersonal_Id(Long ordenClinicaId, Long personalId);
}
