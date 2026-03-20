/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.FarmaciaMedicamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface FarmaciaMedicamentoRepository extends JpaRepository<FarmaciaMedicamento, Long> {

    Optional<FarmaciaMedicamento> findFirstByCodigoBarrasIgnoreCaseAndActivoTrue(String codigoBarras);

    @Query("SELECT COUNT(m) FROM FarmaciaMedicamento m WHERE m.activo = true")
    long countActivos();

    @Query("SELECT COUNT(m) FROM FarmaciaMedicamento m WHERE m.activo = true AND m.stockMinimo IS NOT NULL AND m.cantidad <= m.stockMinimo")
    long countStockBajo();

    @Query("SELECT COUNT(m) FROM FarmaciaMedicamento m WHERE m.activo = true AND m.fechaVencimiento IS NOT NULL AND m.fechaVencimiento >= :desde AND m.fechaVencimiento <= :hasta")
    long countProximosAVencer(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT m FROM FarmaciaMedicamento m
            WHERE m.activo = true
              AND (:soloStock = false OR m.cantidad > 0)
              AND ((:q = '') OR LOWER(m.nombre) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(m.lote, '')) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(m.codigoBarras, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<FarmaciaMedicamento> searchPaged(@Param("q") String q, @Param("soloStock") boolean soloStock, Pageable pageable);
}
