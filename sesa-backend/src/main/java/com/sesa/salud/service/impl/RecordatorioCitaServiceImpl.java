/**
 * Implementación de recordatorios automáticos e inteligentes de citas.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.entity.Cita;
import com.sesa.salud.entity.Notificacion;
import com.sesa.salud.entity.NotificacionDestinatario;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.entity.Usuario;
import com.sesa.salud.repository.CitaRepository;
import com.sesa.salud.repository.NotificacionDestinatarioRepository;
import com.sesa.salud.repository.NotificacionRepository;
import com.sesa.salud.repository.UsuarioRepository;
import com.sesa.salud.service.RecordatorioCitaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecordatorioCitaServiceImpl implements RecordatorioCitaService {

    private static final long REMITENTE_SISTEMA = 0L;
    private static final String REMITENTE_NOMBRE = "Sistema - Recordatorios";
    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CitaRepository citaRepository;
    private final NotificacionRepository notificacionRepository;
    private final NotificacionDestinatarioRepository destinatarioRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public int procesarRecordatoriosDelTenant() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime now = LocalDateTime.now(zone);
        int enviados = 0;

        // Ventana 24h: citas entre now+23h y now+25h
        LocalDateTime inicio24 = now.plusHours(23);
        LocalDateTime fin24 = now.plusHours(25);
        List<Cita> citas24h = citaRepository.findParaRecordatorio24h(inicio24, fin24);
        for (Cita cita : citas24h) {
            if (enviarRecordatorio(cita, "24h", "Tienes una cita mañana",
                    "Recordatorio: tu cita de " + cita.getServicio() + " es el " + cita.getFechaHora().format(FMT_FECHA) + ".")) {
                cita.setRecordatorio24hEnviadoAt(Instant.now());
                citaRepository.save(cita);
                enviados++;
            }
        }

        // Ventana 1h: citas entre now+50min y now+70min
        LocalDateTime inicio1h = now.plusMinutes(50);
        LocalDateTime fin1h = now.plusMinutes(70);
        List<Cita> citas1h = citaRepository.findParaRecordatorio1h(inicio1h, fin1h);
        for (Cita cita : citas1h) {
            if (enviarRecordatorio(cita, "1h", "Tu cita es en aproximadamente 1 hora",
                    "Recordatorio: tu cita de " + cita.getServicio() + " es a las " + cita.getFechaHora().format(FMT_FECHA) + ". ¡No olvides asistir!")) {
                cita.setRecordatorio1hEnviadoAt(Instant.now());
                citaRepository.save(cita);
                enviados++;
            }
        }

        if (enviados > 0) {
            log.info("Recordatorios de cita enviados: {} (tenant actual)", enviados);
        }
        return enviados;
    }

    private boolean enviarRecordatorio(Cita cita, String ventana, String titulo, String contenido) {
        Paciente paciente = cita.getPaciente();
        Long usuarioId = paciente.getUsuarioId();
        if (usuarioId == null) {
            log.debug("Cita id={} sin usuario vinculado al paciente; no se envía recordatorio {}", cita.getId(), ventana);
            return false;
        }
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) {
            return false;
        }
        Notificacion notif = Notificacion.builder()
                .titulo(titulo)
                .contenido(contenido)
                .tipo("RECORDATORIO_CITA")
                .remitenteId(REMITENTE_SISTEMA)
                .remitenteNombre(REMITENTE_NOMBRE)
                .fechaEnvio(Instant.now())
                .citaId(cita.getId())
                .build();
        notif = notificacionRepository.save(notif);
        NotificacionDestinatario dest = NotificacionDestinatario.builder()
                .notificacion(notif)
                .usuarioId(usuario.getId())
                .usuarioEmail(usuario.getEmail())
                .usuarioNombre(usuario.getNombreCompleto())
                .leido(false)
                .build();
        destinatarioRepository.save(dest);
        return true;
    }
}
