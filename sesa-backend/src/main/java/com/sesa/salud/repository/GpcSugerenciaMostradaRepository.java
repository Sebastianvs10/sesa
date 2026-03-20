/**
 * S15: Repositorio registro de visualización GPC (auditoría).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.GpcSugerenciaMostrada;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GpcSugerenciaMostradaRepository extends JpaRepository<GpcSugerenciaMostrada, Long> {
}
