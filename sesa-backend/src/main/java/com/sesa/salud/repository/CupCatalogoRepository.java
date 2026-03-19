/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.CupCatalogo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CupCatalogoRepository extends JpaRepository<CupCatalogo, Long> {

    List<CupCatalogo> findByActivoTrueOrderByCodigoAsc();

    Optional<CupCatalogo> findByCodigo(String codigo);

    @Query("SELECT c FROM CupCatalogo c WHERE c.activo = true AND " +
           "(LOWER(c.codigo) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY c.codigo")
    List<CupCatalogo> buscarActivos(@Param("q") String q, Pageable pageable);

    @Query("SELECT c FROM CupCatalogo c WHERE c.activo = true AND " +
           "(LOWER(c.codigo) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY c.codigo")
    List<CupCatalogo> buscarActivos(@Param("q") String q);

    Page<CupCatalogo> findByActivoTrueAndTipoServicioOrderByCodigoAsc(String tipoServicio, Pageable pageable);
}
