/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.NotificacionDestinatario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificacionDestinatarioRepository extends JpaRepository<NotificacionDestinatario, Long> {

    Page<NotificacionDestinatario> findByUsuarioIdAndArchivadoFalseAndEliminadoFalseOrderByNotificacion_FechaEnvioDesc(
            Long usuarioId, Pageable pageable);

    Page<NotificacionDestinatario> findByUsuarioIdAndArchivadoTrueAndEliminadoFalseOrderByNotificacion_FechaEnvioDesc(
            Long usuarioId, Pageable pageable);

    Page<NotificacionDestinatario> findByUsuarioIdAndEliminadoTrueOrderByNotificacion_FechaEnvioDesc(
            Long usuarioId, Pageable pageable);

    long countByUsuarioIdAndLeidoFalseAndArchivadoFalseAndEliminadoFalse(Long usuarioId);

    Optional<NotificacionDestinatario> findByNotificacionIdAndUsuarioId(Long notificacionId, Long usuarioId);
}
