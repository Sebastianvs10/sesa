/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.FarmaciaMedicamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmaciaMedicamentoRepository extends JpaRepository<FarmaciaMedicamento, Long> {
}
