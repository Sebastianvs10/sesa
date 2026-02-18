/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesa.salud.dto.SyncBatchRequest.SyncOperationItem;
import com.sesa.salud.dto.SyncBatchResponse.SyncItemResult;
import com.sesa.salud.entity.Consulta;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.entity.Personal;
import com.sesa.salud.repository.ConsultaRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.repository.PersonalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Handler de sincronización offline para la entidad Consultas.
 */
@Component("consultas")
@RequiredArgsConstructor
@Slf4j
public class ConsultaSyncHandler implements SyncEntityHandler {

    private final ConsultaRepository consultaRepository;
    private final PacienteRepository pacienteRepository;
    private final PersonalRepository personalRepository;
    private final ObjectMapper objectMapper;

    @Override
    public SyncItemResult handle(SyncOperationItem op, String userEmail) {
        String method = op.getMethod().toUpperCase();
        if (!"POST".equals(method)) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(405)
                    .error("Consultas solo soporta POST en sync offline").build();
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = op.getBody() instanceof Map
                    ? (Map<String, Object>) op.getBody()
                    : objectMapper.convertValue(op.getBody(), Map.class);

            Long pacienteId = toLong(body.get("pacienteId"));
            if (pacienteId == null) {
                return SyncItemResult.builder()
                        .clientId(op.getClientId()).success(false).status(400)
                        .error("pacienteId es obligatorio").build();
            }

            Paciente paciente = pacienteRepository.findById(pacienteId).orElse(null);
            if (paciente == null) {
                return SyncItemResult.builder()
                        .clientId(op.getClientId()).success(false).status(404)
                        .error("Paciente no encontrado: " + pacienteId).build();
            }

            Consulta c = Consulta.builder()
                    .paciente(paciente)
                    .motivoConsulta((String) body.get("motivoConsulta"))
                    .enfermedadActual((String) body.get("enfermedadActual"))
                    .antecedentesPersonales((String) body.get("antecedentesPersonales"))
                    .antecedentesFamiliares((String) body.get("antecedentesFamiliares"))
                    .alergias((String) body.get("alergias"))
                    .fechaConsulta(Instant.now())
                    .build();

            Long profesionalId = toLong(body.get("profesionalId"));
            if (profesionalId != null) {
                personalRepository.findById(profesionalId).ifPresent(c::setProfesional);
            }

            c = consultaRepository.save(c);

            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(true).status(201)
                    .serverId(c.getId()).build();

        } catch (Exception e) {
            log.error("Error en ConsultaSyncHandler: {}", e.getMessage(), e);
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(500)
                    .error("Error procesando consulta: " + e.getMessage()).build();
        }
    }

    private Long toLong(Object val) {
        if (val instanceof Number) return ((Number) val).longValue();
        if (val instanceof String) {
            try { return Long.parseLong((String) val); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }
}
