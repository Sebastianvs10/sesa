/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.ImagenDiagnostica;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ImagenDiagnosticaRepository extends JpaRepository<ImagenDiagnostica, Long>, JpaSpecificationExecutor<ImagenDiagnostica> {
    List<ImagenDiagnostica> findByAtencion_IdOrderByCreatedAtDesc(Long atencionId, Pageable pageable);

    @Query("SELECT DISTINCT i FROM ImagenDiagnostica i JOIN FETCH i.atencion a JOIN FETCH a.historiaClinica h JOIN FETCH h.paciente p WHERE i.id IN :ids")
    List<ImagenDiagnostica> findAllByIdWithAtencionPaciente(@Param("ids") List<Long> ids);
}
