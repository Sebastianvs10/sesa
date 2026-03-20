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
    /**
     * Items se cargan en batch (@BatchSize) para evitar HHH90003004 en resultados paginados.
     */
    @Query("SELECT o FROM OrdenClinica o WHERE o.paciente.id = :pacienteId ORDER BY o.createdAt DESC")
    List<OrdenClinica> findByPaciente_IdOrderByCreatedAtDesc(@Param("pacienteId") Long pacienteId, Pageable pageable);

    /** S14: Órdenes del paciente que ya tienen resultado (para portal). */
    @Query("SELECT o FROM OrdenClinica o WHERE o.paciente.id = :pacienteId AND o.resultado IS NOT NULL AND o.resultado != '' ORDER BY COALESCE(o.fechaResultado, o.createdAt) DESC")
    List<OrdenClinica> findByPaciente_IdAndResultadoNotNullOrderByFechaResultadoDesc(@Param("pacienteId") Long pacienteId);

    List<OrdenClinica> findByConsulta_IdOrderByCreatedAtAsc(Long consultaId);
    List<OrdenClinica> findByTipoOrderByCreatedAtDesc(String tipo, Pageable pageable);

    @Query("SELECT o FROM OrdenClinica o WHERE o.tipo = :tipo AND (o.estadoDispensacionFarmacia IS NULL OR o.estadoDispensacionFarmacia IN :estados) ORDER BY o.createdAt DESC")
    List<OrdenClinica> findOrdenesMedicamentoPendientesDispensar(@Param("tipo") String tipo, @Param("estados") List<String> estados, Pageable pageable);

    /** @deprecated Usar {@link #findOrdenesFarmaciaPendientesPage}. Items cargados por @BatchSize. */
    @Deprecated
    @Query("SELECT o FROM OrdenClinica o WHERE o.tipo IN ('MEDICAMENTO', 'COMPUESTA') AND (o.estadoDispensacionFarmacia IS NULL OR o.estadoDispensacionFarmacia IN :estados) ORDER BY o.createdAt DESC")
    List<OrdenClinica> findOrdenesFarmaciaPendientes(@Param("estados") List<String> estados, Pageable pageable);

    /**
     * Misma lógica que {@link #findOrdenesFarmaciaPendientes} pero con filtro de negocio en BD y paginación correcta.
     * {@code q} opcional: paciente, documento, detalle, ítems, médico (consulta), id de orden.
     */
    @Query(
        value = """
            SELECT o FROM OrdenClinica o
            WHERE (o.estadoDispensacionFarmacia IS NULL OR o.estadoDispensacionFarmacia IN :estados)
            AND (
              UPPER(o.tipo) = 'MEDICAMENTO'
              OR (UPPER(o.tipo) = 'COMPUESTA' AND EXISTS (
                SELECT 1 FROM OrdenClinicaItem i0 WHERE i0.orden = o AND UPPER(i0.tipo) = 'MEDICAMENTO'
              ))
            )
            AND (
              :q IS NULL OR :q = ''
              OR LOWER(CONCAT(COALESCE(o.paciente.nombres,''), ' ', COALESCE(o.paciente.apellidos,''))) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(COALESCE(o.paciente.documento,'')) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(COALESCE(o.paciente.tipoDocumento,'')) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(COALESCE(o.detalle,'')) LIKE LOWER(CONCAT('%', :q, '%'))
              OR EXISTS (SELECT 1 FROM OrdenClinicaItem it WHERE it.orden = o AND LOWER(COALESCE(it.detalle,'')) LIKE LOWER(CONCAT('%', :q, '%')))
              OR LOWER(CONCAT(COALESCE(o.consulta.profesional.nombres,''), ' ', COALESCE(o.consulta.profesional.apellidos,''))) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            ORDER BY o.createdAt DESC
            """,
        countQuery = """
            SELECT COUNT(o) FROM OrdenClinica o
            WHERE (o.estadoDispensacionFarmacia IS NULL OR o.estadoDispensacionFarmacia IN :estados)
            AND (
              UPPER(o.tipo) = 'MEDICAMENTO'
              OR (UPPER(o.tipo) = 'COMPUESTA' AND EXISTS (
                SELECT 1 FROM OrdenClinicaItem i0 WHERE i0.orden = o AND UPPER(i0.tipo) = 'MEDICAMENTO'
              ))
            )
            AND (
              :q IS NULL OR :q = ''
              OR LOWER(CONCAT(COALESCE(o.paciente.nombres,''), ' ', COALESCE(o.paciente.apellidos,''))) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(COALESCE(o.paciente.documento,'')) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(COALESCE(o.paciente.tipoDocumento,'')) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(COALESCE(o.detalle,'')) LIKE LOWER(CONCAT('%', :q, '%'))
              OR EXISTS (SELECT 1 FROM OrdenClinicaItem it WHERE it.orden = o AND LOWER(COALESCE(it.detalle,'')) LIKE LOWER(CONCAT('%', :q, '%')))
              OR LOWER(CONCAT(COALESCE(o.consulta.profesional.nombres,''), ' ', COALESCE(o.consulta.profesional.apellidos,''))) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            """
    )
    /**
     * Paginación a nivel de BD: "items" se excluye del EntityGraph para evitar el warning
     * HHH90003004 (in-memory pagination). Los ítems se cargan en batch gracias a
     * @BatchSize(size=30) en OrdenClinica.items → una sola query IN por página.
     */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"paciente", "consulta", "consulta.profesional"})
    Page<OrdenClinica> findOrdenesFarmaciaPendientesPage(@Param("estados") List<String> estados, @Param("q") String q, Pageable pageable);

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
