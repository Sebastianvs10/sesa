/**
 * S15: Repositorio Guías de práctica clínica.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.GuiaGpc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuiaGpcRepository extends JpaRepository<GuiaGpc, Long> {

    List<GuiaGpc> findByCodigoCie10OrderByTituloAsc(String codigoCie10);

    /** Búsqueda por prefijo de código (ej. E11 para E11.9). */
    List<GuiaGpc> findByCodigoCie10StartingWithOrderByTituloAsc(String codigoPrefix);
}
