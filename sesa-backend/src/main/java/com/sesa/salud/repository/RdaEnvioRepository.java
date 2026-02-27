/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.RdaEnvio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RdaEnvioRepository extends JpaRepository<RdaEnvio, Long> {

    List<RdaEnvio> findByAtencionIdOrderByFechaGeneracionDesc(Long atencionId);

    Optional<RdaEnvio> findFirstByAtencionIdAndTipoRdaOrderByFechaGeneracionDesc(
            Long atencionId, RdaEnvio.TipoRda tipoRda);

    List<RdaEnvio> findByEstadoEnvioAndTenantSchema(
            RdaEnvio.EstadoRda estadoEnvio, String tenantSchema);

    boolean existsByAtencionIdAndEstadoEnvio(Long atencionId, RdaEnvio.EstadoRda estadoEnvio);
}
