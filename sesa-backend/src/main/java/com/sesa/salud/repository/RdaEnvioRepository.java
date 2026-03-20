/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.RdaEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RdaEnvioRepository extends JpaRepository<RdaEnvio, Long> {

    List<RdaEnvio> findByAtencionIdOrderByFechaGeneracionDesc(Long atencionId);

    Optional<RdaEnvio> findFirstByAtencionIdAndTipoRdaOrderByFechaGeneracionDesc(
            Long atencionId, RdaEnvio.TipoRda tipoRda);

    /** S11: RDA por urgencia. */
    Optional<RdaEnvio> findFirstByUrgenciaRegistroIdAndTipoRdaOrderByFechaGeneracionDesc(
            Long urgenciaRegistroId, RdaEnvio.TipoRda tipoRda);

    /** S11: RDA por hospitalización. */
    Optional<RdaEnvio> findFirstByHospitalizacionIdAndTipoRdaOrderByFechaGeneracionDesc(
            Long hospitalizacionId, RdaEnvio.TipoRda tipoRda);

    List<RdaEnvio> findByEstadoEnvioAndTenantSchema(
            RdaEnvio.EstadoRda estadoEnvio, String tenantSchema);

    boolean existsByAtencionIdAndEstadoEnvio(Long atencionId, RdaEnvio.EstadoRda estadoEnvio);

    /** S4: atenciones con RDA enviado o confirmado en el conjunto dado. */
    @Query("SELECT DISTINCT e.atencionId FROM RdaEnvio e WHERE e.atencionId IN :atencionIds AND e.estadoEnvio IN :estados")
    List<Long> findDistinctAtencionIdsByAtencionIdInAndEstadoEnvioIn(
            @Param("atencionIds") Collection<Long> atencionIds,
            @Param("estados") List<RdaEnvio.EstadoRda> estados);
}
