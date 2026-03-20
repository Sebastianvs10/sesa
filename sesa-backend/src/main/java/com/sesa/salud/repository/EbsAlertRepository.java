/**
 * Repositorio EBS: alertas epidemiológicas/geográficas.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.EbsAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EbsAlertRepository extends JpaRepository<EbsAlert, Long> {

    List<EbsAlert> findByStatusOrderByAlertDateDesc(String status);

    List<EbsAlert> findAllByOrderByAlertDateDesc();

    List<EbsAlert> findByMunicipioCodigoAndAlertDateBetween(String municipioCodigo, LocalDate from, LocalDate to);
}
