/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.NotaEnfermeria;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotaEnfermeriaRepository extends JpaRepository<NotaEnfermeria, Long> {

    List<NotaEnfermeria> findByAtencion_IdOrderByFechaNotaDesc(Long atencionId, Pageable pageable);
}
