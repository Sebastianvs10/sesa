/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.UrgenciaRegistro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UrgenciaRegistroRepository extends JpaRepository<UrgenciaRegistro, Long> {

    Page<UrgenciaRegistro> findByEstado(String estado, Pageable pageable);

    List<UrgenciaRegistro> findByEstadoIn(List<String> estados, Pageable pageable);
}
