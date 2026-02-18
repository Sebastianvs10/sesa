/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    Page<Notificacion> findByRemitenteIdOrderByFechaEnvioDesc(Long remitenteId, Pageable pageable);

    Page<Notificacion> findAllByOrderByFechaEnvioDesc(Pageable pageable);
}
