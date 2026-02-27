/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.ProcedimientoCatalogo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcedimientoCatalogoRepository extends JpaRepository<ProcedimientoCatalogo, Long> {

    List<ProcedimientoCatalogo> findByActivoTrueOrderByCategoria();

    Page<ProcedimientoCatalogo> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre, Pageable pageable);
}
