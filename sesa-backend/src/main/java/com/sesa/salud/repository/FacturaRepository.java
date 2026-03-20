/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.Factura;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface FacturaRepository extends JpaRepository<Factura, Long>, JpaSpecificationExecutor<Factura> {

    List<Factura> findByPaciente_IdOrderByFechaFacturaDesc(Long pacienteId, Pageable pageable);

    List<Factura> findByFechaFacturaBetween(Instant desde, Instant hasta);

    List<Factura> findByEstadoIn(List<String> estados);

    @Query("SELECT COALESCE(SUM(f.valorTotal), 0) FROM Factura f WHERE f.estado = :estado")
    BigDecimal sumByEstado(@Param("estado") String estado);

    @Query("SELECT COUNT(f) FROM Factura f WHERE f.estado = :estado")
    long countByEstado(@Param("estado") String estado);

    @Query("SELECT COALESCE(SUM(f.valorTotal), 0) FROM Factura f WHERE f.fechaFactura >= :desde AND f.fechaFactura < :hasta")
    BigDecimal sumByFechaBetween(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("SELECT COUNT(f) FROM Factura f WHERE f.fechaFactura >= :desde AND f.fechaFactura < :hasta")
    long countByFechaBetween(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("SELECT COUNT(f) > 0 FROM Factura f WHERE f.paciente.id = :pacienteId AND f.estado = 'PENDIENTE'")
    boolean existeFacturaPendienteByPacienteId(@Param("pacienteId") Long pacienteId);

    /** Siguiente consecutivo para número de factura (usa secuencia factura_seq por tenant). */
    @org.springframework.data.jpa.repository.Query(value = "SELECT nextval('factura_seq')", nativeQuery = true)
    long getNextConsecutive();

    boolean existsByOrden_Id(Long ordenId);
}
