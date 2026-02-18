/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.NotificacionCreateRequest;
import com.sesa.salud.dto.NotificacionDto;
import com.sesa.salud.entity.Notificacion;
import com.sesa.salud.entity.NotificacionAdjunto;
import com.sesa.salud.entity.NotificacionDestinatario;
import com.sesa.salud.entity.Usuario;
import com.sesa.salud.repository.NotificacionAdjuntoRepository;
import com.sesa.salud.repository.NotificacionDestinatarioRepository;
import com.sesa.salud.repository.NotificacionRepository;
import com.sesa.salud.repository.UsuarioRepository;
import com.sesa.salud.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final NotificacionDestinatarioRepository destinatarioRepository;
    private final NotificacionAdjuntoRepository adjuntoRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public NotificacionDto create(NotificacionCreateRequest request, Long remitenteId, String remitenteNombre) {
        Notificacion notificacion = Notificacion.builder()
                .titulo(request.getTitulo())
                .contenido(request.getContenido())
                .tipo(request.getTipo() != null ? request.getTipo() : "GENERAL")
                .remitenteId(remitenteId)
                .remitenteNombre(remitenteNombre)
                .fechaEnvio(Instant.now())
                .build();

        List<NotificacionDestinatario> destinatarios = new ArrayList<>();
        for (Long destinatarioId : request.getDestinatarioIds()) {
            Usuario usuario = usuarioRepository.findById(destinatarioId)
                    .orElseThrow(() -> new RuntimeException("Usuario destinatario no encontrado: " + destinatarioId));

            NotificacionDestinatario dest = NotificacionDestinatario.builder()
                    .notificacion(notificacion)
                    .usuarioId(usuario.getId())
                    .usuarioEmail(usuario.getEmail())
                    .usuarioNombre(usuario.getNombreCompleto())
                    .leido(false)
                    .build();
            destinatarios.add(dest);
        }
        notificacion.setDestinatarios(destinatarios);

        notificacion = notificacionRepository.save(notificacion);
        log.info("Notificación creada id={} por remitente={}", notificacion.getId(), remitenteId);
        return toDto(notificacion);
    }

    @Override
    @Transactional
    public void addAdjunto(Long notificacionId, String nombreArchivo, String contentType, byte[] datos) {
        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada: " + notificacionId));

        NotificacionAdjunto adjunto = NotificacionAdjunto.builder()
                .notificacion(notificacion)
                .nombreArchivo(nombreArchivo)
                .contentType(contentType)
                .tamano((long) datos.length)
                .datos(datos)
                .build();

        adjuntoRepository.save(adjunto);
        log.info("Adjunto '{}' agregado a notificación id={}", nombreArchivo, notificacionId);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificacionDto getById(Long id) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada: " + id));
        return toDto(notificacion);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificacionDto> listEnviadas(Long remitenteId, Pageable pageable) {
        return notificacionRepository.findByRemitenteIdOrderByFechaEnvioDesc(remitenteId, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificacionDto> listRecibidas(Long usuarioId, Pageable pageable) {
        return destinatarioRepository.findByUsuarioIdOrderByNotificacion_FechaEnvioDesc(usuarioId, pageable)
                .map(dest -> toDto(dest.getNotificacion()));
    }

    @Override
    @Transactional
    public void marcarLeida(Long notificacionId, Long usuarioId) {
        NotificacionDestinatario dest = destinatarioRepository
                .findByNotificacionIdAndUsuarioId(notificacionId, usuarioId)
                .orElseThrow(() -> new RuntimeException(
                        "Destinatario no encontrado para notificación " + notificacionId + " y usuario " + usuarioId));

        if (!dest.getLeido()) {
            dest.setLeido(true);
            dest.setFechaLectura(Instant.now());
            destinatarioRepository.save(dest);
            log.info("Notificación id={} marcada como leída por usuario={}", notificacionId, usuarioId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countNoLeidas(Long usuarioId) {
        return destinatarioRepository.countByUsuarioIdAndLeidoFalse(usuarioId);
    }

    private NotificacionDto toDto(Notificacion n) {
        List<NotificacionDto.AdjuntoInfo> adjuntosDto = n.getAdjuntos() != null
                ? n.getAdjuntos().stream().map(a -> NotificacionDto.AdjuntoInfo.builder()
                        .id(a.getId())
                        .nombreArchivo(a.getNombreArchivo())
                        .contentType(a.getContentType())
                        .tamano(a.getTamano())
                        .build())
                .collect(Collectors.toList())
                : List.of();

        List<NotificacionDto.DestinatarioInfo> destinatariosDto = n.getDestinatarios() != null
                ? n.getDestinatarios().stream().map(d -> NotificacionDto.DestinatarioInfo.builder()
                        .usuarioId(d.getUsuarioId())
                        .usuarioEmail(d.getUsuarioEmail())
                        .usuarioNombre(d.getUsuarioNombre())
                        .leido(d.getLeido())
                        .fechaLectura(d.getFechaLectura())
                        .build())
                .collect(Collectors.toList())
                : List.of();

        return NotificacionDto.builder()
                .id(n.getId())
                .titulo(n.getTitulo())
                .contenido(n.getContenido())
                .tipo(n.getTipo())
                .remitenteId(n.getRemitenteId())
                .remitenteNombre(n.getRemitenteNombre())
                .fechaEnvio(n.getFechaEnvio())
                .adjuntos(adjuntosDto)
                .destinatarios(destinatariosDto)
                .build();
    }
}
