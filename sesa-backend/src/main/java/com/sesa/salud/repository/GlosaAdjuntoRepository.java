/**
 * S9: Adjuntos de glosa.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.GlosaAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GlosaAdjuntoRepository extends JpaRepository<GlosaAdjunto, Long> {

    List<GlosaAdjunto> findByGlosaIdOrderByCreatedAtAsc(Long glosaId);
}
