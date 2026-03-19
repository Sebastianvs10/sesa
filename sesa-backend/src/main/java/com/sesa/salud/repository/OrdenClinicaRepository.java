/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.OrdenClinica;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrdenClinicaRepository extends JpaRepository<OrdenClinica, Long> {
    @Query("SELECT o FROM OrdenClinica o WHERE o.paciente.id = :pacienteId ORDER BY o.createdAt DESC")
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"items"})
    List<OrdenClinica> findByPaciente_IdOrderByCreatedAtDesc(@Param("pacienteId") Long pacienteId, Pageable pageable);

    /** S14: Órdenes del paciente que ya tienen resultado (para portal). */
    @Query("SELECT o FROM OrdenClinica o WHERE o.paciente.id = :pacienteId AND o.resultado IS NOT NULL AND o.resultado != '' ORDER BY COALESCE(o.fechaResultado, o.createdAt) DESC")
    List<OrdenClinica> findByPaciente_IdAndResultadoNotNullOrderByFechaResultadoDesc(@Param("pacienteId") Long pacienteId);

    List<OrdenClinica> findByConsulta_IdOrderByCreatedAtAsc(Long consultaId);
    List<OrdenClinica> findByTipoOrderByCreatedAtDesc(String tipo, Pageable pageable);

    @Query("SELECT o FROM OrdenClinica o WHERE o.tipo = :tipo AND (o.estadoDispensacionFarmacia IS NULL OR o.estadoDispensacionFarmacia IN :estados) ORDER BY o.createdAt DESC")
    List<OrdenClinica> findOrdenesMedicamentoPendientesDispensar(@Param("tipo") String tipo, @Param("estados") List<String> estados, Pageable pageable);

    /** Órdenes MEDICAMENTO o COMPUESTA (con ítems) pendientes/parciales de dispensar en farmacia. */
    @Query("SELECT o FROM OrdenClinica o WHERE o.tipo IN ('MEDICAMENTO', 'COMPUESTA') AND (o.estadoDispensacionFarmacia IS NULL OR o.estadoDispensacionFarmacia IN :estados) ORDER BY o.createdAt DESC")
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"items"})
    List<OrdenClinica> findOrdenesFarmaciaPendientes(@Param("estados") List<String> estados, Pageable pageable);

    /** S4: cantidad de órdenes con resultado crítico sin ningún registro de lectura. */
    @Query("SELECT COUNT(o) FROM OrdenClinica o WHERE o.resultadoCritico = true AND NOT EXISTS (SELECT 1 FROM com.sesa.salud.entity.ResultadoCriticoLectura r WHERE r.ordenClinica.id = o.id)")
    long countByResultadoCriticoTrueAndSinLectura();

    /** Órdenes que aún no tienen factura asociada (pendientes de facturar). Incluye medicamentos, laboratorio, procedimientos, etc. */
    @Query(
        value = "SELECT o FROM OrdenClinica o WHERE NOT EXISTS (SELECT 1 FROM com.sesa.salud.entity.Factura f WHERE f.orden.id = o.id) ORDER BY o.createdAt DESC",
        countQuery = "SELECT COUNT(o) FROM OrdenClinica o WHERE NOT EXISTS (SELECT 1 FROM com.sesa.salud.entity.Factura f WHERE f.orden.id = o.id)"
    )
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"paciente", "consulta"})
    Page<OrdenClinica> findPendientesDeFacturar(Pageable pageable);
}
