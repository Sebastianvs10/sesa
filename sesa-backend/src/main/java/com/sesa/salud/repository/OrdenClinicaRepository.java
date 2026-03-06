/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.OrdenClinica;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrdenClinicaRepository extends JpaRepository<OrdenClinica, Long> {
    List<OrdenClinica> findByPaciente_IdOrderByCreatedAtDesc(Long pacienteId, Pageable pageable);
    List<OrdenClinica> findByConsulta_IdOrderByCreatedAtAsc(Long consultaId);
    List<OrdenClinica> findByTipoOrderByCreatedAtDesc(String tipo, Pageable pageable);

    @Query("SELECT o FROM OrdenClinica o WHERE o.tipo = :tipo AND (o.estadoDispensacionFarmacia IS NULL OR o.estadoDispensacionFarmacia IN :estados) ORDER BY o.createdAt DESC")
    List<OrdenClinica> findOrdenesMedicamentoPendientesDispensar(@Param("tipo") String tipo, @Param("estados") List<String> estados, Pageable pageable);
}
