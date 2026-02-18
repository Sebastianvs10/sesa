/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.NotificacionCreateRequest;
import com.sesa.salud.dto.NotificacionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificacionService {

    NotificacionDto create(NotificacionCreateRequest request, Long remitenteId, String remitenteNombre);

    void addAdjunto(Long notificacionId, String nombreArchivo, String contentType, byte[] datos);

    NotificacionDto getById(Long id);

    Page<NotificacionDto> listEnviadas(Long remitenteId, Pageable pageable);

    Page<NotificacionDto> listRecibidas(Long usuarioId, Pageable pageable);

    void marcarLeida(Long notificacionId, Long usuarioId);

    long countNoLeidas(Long usuarioId);
}
