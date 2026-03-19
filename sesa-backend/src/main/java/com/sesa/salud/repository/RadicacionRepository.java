/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.Radicacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface RadicacionRepository extends JpaRepository<Radicacion, Long> {

    List<Radicacion> findByFactura_IdOrderByFechaRadicacionDesc(Long facturaId);

    @Query("SELECT r FROM Radicacion r WHERE (:estado IS NULL OR r.estado = :estado) " +
           "AND (:desde IS NULL OR r.fechaRadicacion >= :desde) AND (:hasta IS NULL OR r.fechaRadicacion <= :hasta) " +
           "AND (:facturaId IS NULL OR r.factura.id = :facturaId) ORDER BY r.fechaRadicacion DESC")
    Page<Radicacion> findAllFiltered(@Param("estado") String estado,
                                    @Param("desde") Instant desde,
                                    @Param("hasta") Instant hasta,
                                    @Param("facturaId") Long facturaId,
                                    Pageable pageable);
}
