/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.NotificacionAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionAdjuntoRepository extends JpaRepository<NotificacionAdjunto, Long> {

    List<NotificacionAdjunto> findByNotificacionId(Long notificacionId);
}
