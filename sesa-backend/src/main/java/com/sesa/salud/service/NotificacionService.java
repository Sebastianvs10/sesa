/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.DestinatarioDisponibleDto;
import com.sesa.salud.dto.NotificacionBroadcastResult;
import com.sesa.salud.dto.NotificacionCreateRequest;
import com.sesa.salud.dto.NotificacionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificacionService {

    NotificacionDto create(NotificacionCreateRequest request, Long remitenteId, String remitenteNombre);

    void addAdjunto(Long notificacionId, String nombreArchivo, String contentType, byte[] datos);

    NotificacionDto getById(Long id);

    Page<NotificacionDto> listEnviadas(Long remitenteId, Pageable pageable);

    Page<NotificacionDto> listRecibidas(Long usuarioId, Pageable pageable);

    Page<NotificacionDto> listArchivadas(Long usuarioId, Pageable pageable);

    Page<NotificacionDto> listPapelera(Long usuarioId, Pageable pageable);

    void marcarLeida(Long notificacionId, Long usuarioId);

    /**
     * Marca como leídas todas las notificaciones indicadas para el usuario actual (destinatario).
     * Ignora IDs que no correspondan al usuario o no existan.
     */
    void marcarLeidas(List<Long> notificacionIds, Long usuarioId);

    /** Marca una notificación como no leída para el destinatario actual. */
    void marcarNoLeida(Long notificacionId, Long usuarioId);

    /** Marca varias notificaciones como no leídas para el usuario actual (solo donde es destinatario). */
    void marcarNoLeidas(List<Long> notificacionIds, Long usuarioId);

    void archivar(Long notificacionId, Long usuarioId);

    void desarchivar(Long notificacionId, Long usuarioId);

    void moverAPapelera(Long notificacionId, Long usuarioId);

    void restaurarDePapelera(Long notificacionId, Long usuarioId);

    void eliminarDefinitivo(Long notificacionId, Long usuarioId);

    long countNoLeidas(Long usuarioId);

    /** Elimina un adjunto de una notificación. Solo el remitente de la notificación puede eliminarlo. */
    void deleteAdjunto(Long notificacionId, Long adjuntoId, Long usuarioId);

    /** Lista todos los usuarios activos del schema actual como posibles destinatarios. */
    List<DestinatarioDisponibleDto> getDestinatariosDisponibles();

    /**
     * SUPERADMINISTRADOR: crea la notificación en cada schema de tenant activo
     * y la asigna al usuario con rol ADMIN de cada schema.
     */
    NotificacionBroadcastResult broadcastToAdmins(NotificacionCreateRequest request,
                                                   Long remitenteId,
                                                   String remitenteNombre);
}
