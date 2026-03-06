/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.DestinatarioDisponibleDto;
import com.sesa.salud.dto.NotificacionBroadcastResult;
import com.sesa.salud.dto.NotificacionCreateRequest;
import com.sesa.salud.dto.NotificacionDto;
import com.sesa.salud.entity.Notificacion;
import com.sesa.salud.entity.NotificacionAdjunto;
import com.sesa.salud.entity.NotificacionDestinatario;
import com.sesa.salud.entity.Usuario;
import com.sesa.salud.entity.master.Empresa;
import com.sesa.salud.repository.NotificacionAdjuntoRepository;
import com.sesa.salud.repository.NotificacionDestinatarioRepository;
import com.sesa.salud.repository.NotificacionRepository;
import com.sesa.salud.repository.UsuarioRepository;
import com.sesa.salud.repository.master.EmpresaRepository;
import com.sesa.salud.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository        notificacionRepository;
    private final NotificacionDestinatarioRepository destinatarioRepository;
    private final NotificacionAdjuntoRepository adjuntoRepository;
    private final UsuarioRepository             usuarioRepository;
    private final EmpresaRepository             empresaRepository;
    private final DataSource                    dataSource;

    // ── Crear notificación ────────────────────────────────────────────────────

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
                .citaId(request.getCitaId())
                .build();

        List<NotificacionDestinatario> destinatarios = new ArrayList<>();

        if (request.isBroadcastTodos()) {
            // Enviar a todos los usuarios activos del schema actual
            for (Usuario usuario : usuarioRepository.findByActivoTrue()) {
                destinatarios.add(buildDestinatario(notificacion, usuario));
            }
        } else {
            if (request.getDestinatarioIds() == null || request.getDestinatarioIds().isEmpty()) {
                throw new IllegalArgumentException("Debe haber al menos un destinatario");
            }
            for (Long destinatarioId : request.getDestinatarioIds()) {
                Usuario usuario = usuarioRepository.findById(destinatarioId)
                        .orElseThrow(() -> new RuntimeException("Usuario destinatario no encontrado: " + destinatarioId));
                destinatarios.add(buildDestinatario(notificacion, usuario));
            }
        }

        notificacion.setDestinatarios(destinatarios);
        notificacion = notificacionRepository.save(notificacion);
        log.info("Notificación creada id={} por remitente={} (destinatarios={})",
                notificacion.getId(), remitenteId, destinatarios.size());
        return toDto(notificacion);
    }

    private NotificacionDestinatario buildDestinatario(Notificacion notificacion, Usuario usuario) {
        return NotificacionDestinatario.builder()
                .notificacion(notificacion)
                .usuarioId(usuario.getId())
                .usuarioEmail(usuario.getEmail())
                .usuarioNombre(usuario.getNombreCompleto())
                .leido(false)
                .build();
    }

    // ── Adjuntos ──────────────────────────────────────────────────────────────

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

    // ── Consultas ─────────────────────────────────────────────────────────────

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
                        "Destinatario no encontrado para notificación " + notificacionId));
        if (!dest.getLeido()) {
            dest.setLeido(true);
            dest.setFechaLectura(Instant.now());
            destinatarioRepository.save(dest);
        }
    }

    @Override
    @Transactional
    public void marcarLeidas(List<Long> notificacionIds, Long usuarioId) {
        if (notificacionIds == null || notificacionIds.isEmpty()) return;
        for (Long notificacionId : notificacionIds) {
            try {
                marcarLeida(notificacionId, usuarioId);
            } catch (Exception e) {
                log.debug("No se pudo marcar notificación {} como leída: {}", notificacionId, e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void marcarNoLeida(Long notificacionId, Long usuarioId) {
        NotificacionDestinatario dest = destinatarioRepository
                .findByNotificacionIdAndUsuarioId(notificacionId, usuarioId)
                .orElseThrow(() -> new RuntimeException(
                        "Destinatario no encontrado para notificación " + notificacionId));
        if (Boolean.TRUE.equals(dest.getLeido())) {
            dest.setLeido(false);
            dest.setFechaLectura(null);
            destinatarioRepository.save(dest);
        }
    }

    @Override
    @Transactional
    public void marcarNoLeidas(List<Long> notificacionIds, Long usuarioId) {
        if (notificacionIds == null || notificacionIds.isEmpty()) return;
        for (Long notificacionId : notificacionIds) {
            try {
                marcarNoLeida(notificacionId, usuarioId);
            } catch (Exception e) {
                log.debug("No se pudo marcar notificación {} como no leída: {}", notificacionId, e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void deleteAdjunto(Long notificacionId, Long adjuntoId, Long usuarioId) {
        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada: " + notificacionId));
        if (!notificacion.getRemitenteId().equals(usuarioId)) {
            throw new RuntimeException("Solo el remitente puede eliminar adjuntos de esta notificación");
        }
        NotificacionAdjunto adjunto = adjuntoRepository.findById(adjuntoId)
                .filter(a -> a.getNotificacion().getId().equals(notificacionId))
                .orElseThrow(() -> new RuntimeException("Adjunto no encontrado: " + adjuntoId));
        adjuntoRepository.delete(adjunto);
        log.info("Adjunto id={} eliminado de notificación id={}", adjuntoId, notificacionId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countNoLeidas(Long usuarioId) {
        return destinatarioRepository.countByUsuarioIdAndLeidoFalse(usuarioId);
    }

    // ── Destinatarios disponibles ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<DestinatarioDisponibleDto> getDestinatariosDisponibles() {
        return usuarioRepository.findByActivoTrue().stream()
                .map(u -> DestinatarioDisponibleDto.builder()
                        .id(u.getId())
                        .nombre(u.getNombreCompleto())
                        .email(u.getEmail())
                        .rol(u.getRoles() != null ? u.getRoles().stream().findFirst().orElse("USER") : "USER")
                        .build())
                .toList();
    }

    // ── Broadcast SUPERADMINISTRADOR → admins de todos los schemas ─────────────

    /**
     * Crea la notificación en el schema de cada empresa activa y la asigna al
     * usuario con rol ADMIN de ese schema. Usa JDBC directo con SET search_path
     * para poder operar en múltiples schemas dentro de la misma llamada.
     */
    @Override
    public NotificacionBroadcastResult broadcastToAdmins(NotificacionCreateRequest request,
                                                          Long remitenteId,
                                                          String remitenteNombre) {
        List<Empresa> empresas = empresaRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getActivo()))
                .filter(e -> e.getAdminEmail() != null && !e.getAdminEmail().isBlank())
                .toList();

        Instant now    = Instant.now();
        String  tipo   = request.getTipo() != null ? request.getTipo() : "GENERAL";
        int     total  = 0;
        List<String> errores = new ArrayList<>();

        for (Empresa empresa : empresas) {
            String schema = empresa.getSchemaName();
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("SET search_path = '" + schema + "'");

                    // 1. Insertar notificacion
                    long notifId;
                    try (PreparedStatement pst = conn.prepareStatement(
                            "INSERT INTO notificaciones(titulo, contenido, tipo, remitente_id, " +
                            "remitente_nombre, fecha_envio, created_at) " +
                            "VALUES(?, ?, ?, ?, ?, ?, ?) RETURNING id")) {
                        pst.setString(1, request.getTitulo());
                        pst.setString(2, request.getContenido());
                        pst.setString(3, tipo);
                        pst.setLong(4, remitenteId);
                        pst.setString(5, remitenteNombre);
                        pst.setTimestamp(6, Timestamp.from(now));
                        pst.setTimestamp(7, Timestamp.from(now));
                        ResultSet rs = pst.executeQuery();
                        if (!rs.next()) throw new SQLException("No se pudo insertar la notificación");
                        notifId = rs.getLong(1);
                    }

                    // 2. Buscar usuario ADMIN del schema por email
                    try (PreparedStatement pst = conn.prepareStatement(
                            "SELECT id, email, nombre_completo FROM usuarios " +
                            "WHERE email = ? AND activo = true LIMIT 1")) {
                        pst.setString(1, empresa.getAdminEmail());
                        ResultSet rs = pst.executeQuery();
                        if (rs.next()) {
                            long   userId    = rs.getLong(1);
                            String userEmail = rs.getString(2);
                            String userName  = rs.getString(3);

                            // 3. Insertar destinatario
                            try (PreparedStatement pst2 = conn.prepareStatement(
                                    "INSERT INTO notificacion_destinatarios" +
                                    "(notificacion_id, usuario_id, usuario_email, usuario_nombre, leido) " +
                                    "VALUES(?, ?, ?, ?, false)")) {
                                pst2.setLong(1, notifId);
                                pst2.setLong(2, userId);
                                pst2.setString(3, userEmail);
                                pst2.setString(4, userName);
                                pst2.executeUpdate();
                                total++;
                            }
                        } else {
                            log.warn("No se encontró el admin '{}' en schema '{}'",
                                    empresa.getAdminEmail(), schema);
                        }
                    }
                    conn.commit();
                    log.info("Broadcast enviado al schema '{}'", schema);
                } catch (SQLException e) {
                    conn.rollback();
                    errores.add(empresa.getRazonSocial() + ": " + e.getMessage());
                    log.error("Error en broadcast al schema '{}': {}", schema, e.getMessage());
                }
            } catch (Exception e) {
                errores.add(empresa.getRazonSocial() + ": " + e.getMessage());
                log.error("Error de conexión al schema '{}': {}", schema, e.getMessage());
            }
        }

        return NotificacionBroadcastResult.builder()
                .schemasProcessados(empresas.size() - errores.size())
                .totalDestinatarios(total)
                .errores(errores)
                .build();
    }

    // ── Mapeo entidad → DTO ───────────────────────────────────────────────────

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
                .citaId(n.getCitaId())
                .adjuntos(adjuntosDto)
                .destinatarios(destinatariosDto)
                .build();
    }
}
