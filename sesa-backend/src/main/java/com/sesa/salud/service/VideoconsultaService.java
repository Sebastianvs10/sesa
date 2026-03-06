/**
 * Servicio de videoconsulta — salas y señalización en memoria (WebRTC).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.CrearSalaVideoconsultaRequestDto;
import com.sesa.salud.dto.SalaVideoconsultaDto;
import com.sesa.salud.dto.SignalingEventDto;
import com.sesa.salud.dto.SignalingEventWithIndexDto;
import com.sesa.salud.exception.VideoconsultaTokenInvalidoException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class VideoconsultaService {

    private final Map<String, SalaInfo> salas = new ConcurrentHashMap<>();
    private final Map<String, List<SignalingEventWithIndexDto>> signalingBySala = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> signalingIndexBySala = new ConcurrentHashMap<>();

    public SalaVideoconsultaDto crearSala(CrearSalaVideoconsultaRequestDto request) {
        if (request == null) {
            request = new CrearSalaVideoconsultaRequestDto();
        }
        Long citaId = request.getCitaId();
        Long profesionalId = request.getProfesionalId();
        String salaId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String token = UUID.randomUUID().toString().replace("-", "");
        SalaInfo info = new SalaInfo(salaId, token, citaId, profesionalId, null, false, null, new StringBuilder());
        salas.put(salaId, info);
        signalingBySala.put(salaId, new CopyOnWriteArrayList<>());
        signalingIndexBySala.put(salaId, new AtomicInteger(0));
        return SalaVideoconsultaDto.builder()
                .salaId(salaId)
                .role("creador")
                .token(token)
                .citaId(citaId)
                .profesionalId(profesionalId)
                .build();
    }

    public SalaVideoconsultaDto unirseSala(String salaId, String token) {
        SalaInfo info = salas.get(salaId);
        if (info == null) {
            throw new RuntimeException("Sala no encontrada: " + salaId);
        }
        if (token == null || !token.equals(info.token())) {
            throw new VideoconsultaTokenInvalidoException("Token inválido o expirado");
        }
        return SalaVideoconsultaDto.builder()
                .salaId(salaId)
                .role("participante")
                .token(info.token())
                .citaId(info.citaId())
                .profesionalId(info.profesionalId())
                .pacienteId(info.pacienteId())
                .build();
    }

    public void enviarSignaling(String salaId, String token, SignalingEventDto event) {
        validarTokenSala(salaId, token);
        List<SignalingEventWithIndexDto> list = signalingBySala.get(salaId);
        AtomicInteger idx = signalingIndexBySala.get(salaId);
        int next = idx.incrementAndGet();
        list.add(new SignalingEventWithIndexDto(next, event.getType(), event.getPayload()));
    }

    public List<SignalingEventWithIndexDto> obtenerSignaling(String salaId, String token, int afterIndex) {
        validarTokenSala(salaId, token);
        List<SignalingEventWithIndexDto> list = signalingBySala.get(salaId);
        if (list == null) {
            return List.of();
        }
        return list.stream()
                .filter(e -> e.getIndex() > afterIndex)
                .collect(Collectors.toList());
    }

    private void validarTokenSala(String salaId, String token) {
        SalaInfo info = salas.get(salaId);
        if (info == null) {
            throw new RuntimeException("Sala no encontrada: " + salaId);
        }
        if (token == null || !token.equals(info.token())) {
            throw new VideoconsultaTokenInvalidoException("Token inválido o expirado");
        }
    }

    /** Creador: genera token para el asistente (solo toma de notas). Requiere token de sala. */
    public String habilitarAsistente(String salaId, String token) {
        SalaInfo info = salas.get(salaId);
        if (info == null) throw new RuntimeException("Sala no encontrada: " + salaId);
        if (!token.equals(info.token())) throw new VideoconsultaTokenInvalidoException("Token inválido");
        if (info.tokenAsistente() != null) return info.tokenAsistente();
        String tokenAsistente = UUID.randomUUID().toString().replace("-", "");
        SalaInfo updated = new SalaInfo(info.salaId(), info.token(), info.citaId(), info.profesionalId(), info.pacienteId(), info.consentimientoAsistente(), tokenAsistente, info.notas());
        salas.put(salaId, updated);
        return tokenAsistente;
    }

    /** Paciente: registra consentimiento para que un asistente tome notas. Requiere token de sala. */
    public void registrarConsentimientoAsistente(String salaId, String token) {
        SalaInfo info = salas.get(salaId);
        if (info == null) throw new RuntimeException("Sala no encontrada: " + salaId);
        if (!token.equals(info.token())) throw new VideoconsultaTokenInvalidoException("Token inválido");
        SalaInfo updated = new SalaInfo(info.salaId(), info.token(), info.citaId(), info.profesionalId(), info.pacienteId(), true, info.tokenAsistente(), info.notas());
        salas.put(salaId, updated);
    }

    /** Asistente: guarda el contenido completo de las notas. Requiere token de asistente y consentimiento previo. */
    public void guardarNotas(String salaId, String token, String texto) {
        SalaInfo info = salas.get(salaId);
        if (info == null) throw new RuntimeException("Sala no encontrada: " + salaId);
        if (!Boolean.TRUE.equals(info.consentimientoAsistente()) || !token.equals(info.tokenAsistente())) {
            throw new VideoconsultaTokenInvalidoException("Sin consentimiento o token de asistente inválido");
        }
        StringBuilder notas = new StringBuilder(texto != null ? texto : "");
        SalaInfo updated = new SalaInfo(info.salaId(), info.token(), info.citaId(), info.profesionalId(), info.pacienteId(), true, info.tokenAsistente(), notas);
        salas.put(salaId, updated);
    }

    /** Creador: obtiene el resumen (notas del asistente) de la reunión. */
    public String obtenerNotas(String salaId, String token) {
        SalaInfo info = salas.get(salaId);
        if (info == null) throw new RuntimeException("Sala no encontrada: " + salaId);
        if (!token.equals(info.token())) throw new VideoconsultaTokenInvalidoException("Token inválido");
        return info.notas().toString();
    }

    /** Valida si el token es de asistente y hay consentimiento (para permitir acceso a la vista de notas). */
    public boolean validarAsistente(String salaId, String token) {
        SalaInfo info = salas.get(salaId);
        if (info == null) return false;
        return Boolean.TRUE.equals(info.consentimientoAsistente()) && token != null && token.equals(info.tokenAsistente());
    }

    /** Paciente: indica si el profesional solicitó un asistente y aún no ha dado consentimiento. */
    public boolean solicitudAsistentePendiente(String salaId, String token) {
        SalaInfo info = salas.get(salaId);
        if (info == null) return false;
        if (!token.equals(info.token())) return false;
        return info.tokenAsistente() != null && !info.consentimientoAsistente();
    }

    private record SalaInfo(String salaId, String token, Long citaId, Long profesionalId, Long pacienteId,
                            boolean consentimientoAsistente, String tokenAsistente, StringBuilder notas) {}
}
