/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesa.salud.dto.SyncBatchRequest.SyncOperationItem;
import com.sesa.salud.dto.SyncBatchResponse.SyncItemResult;
import com.sesa.salud.entity.HistoriaClinica;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.repository.HistoriaClinicaRepository;
import com.sesa.salud.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("historias-clinicas")
@RequiredArgsConstructor
@Slf4j
public class HistoriaClinicaSyncHandler implements SyncEntityHandler {

    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final PacienteRepository pacienteRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern ID_PATTERN = Pattern.compile("/historia-clinica/(\\d+)");

    @Override
    public SyncItemResult handle(SyncOperationItem op, String userEmail) {
        String method = op.getMethod().toUpperCase();
        try {
            return switch (method) {
                case "POST" -> handleCreate(op);
                case "PUT", "PATCH" -> handleUpdate(op);
                default -> SyncItemResult.builder()
                        .clientId(op.getClientId()).success(false).status(405)
                        .error("Método no soportado: " + method).build();
            };
        } catch (Exception e) {
            log.error("Error en HistoriaClinicaSyncHandler: {}", e.getMessage(), e);
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(500)
                    .error("Error procesando historia clínica: " + e.getMessage()).build();
        }
    }

    private SyncItemResult handleCreate(SyncOperationItem op) throws Exception {
        Map<String, Object> body = toMap(op.getBody());

        Long pacienteId = toLong(body.get("pacienteId"));
        if (pacienteId == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("pacienteId es obligatorio").build();
        }

        // Conflicto: si ya existe historia para este paciente, devolver la existente
        Optional<HistoriaClinica> existing = historiaClinicaRepository.findByPacienteId(pacienteId);
        if (existing.isPresent()) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(true).status(200)
                    .serverId(existing.get().getId()).build();
        }

        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + pacienteId));

        HistoriaClinica hc = HistoriaClinica.builder()
                .paciente(paciente)
                .fechaApertura(Instant.now())
                .estado("ACTIVA")
                .grupoSanguineo((String) body.get("grupoSanguineo"))
                .alergiasGenerales((String) body.get("alergiasGenerales"))
                .antecedentesPersonales((String) body.get("antecedentesPersonales"))
                .antecedentesQuirurgicos((String) body.get("antecedentesQuirurgicos"))
                .antecedentesFarmacologicos((String) body.get("antecedentesFarmacologicos"))
                .antecedentesTraumaticos((String) body.get("antecedentesTraumaticos"))
                .antecedentesGinecoobstetricos((String) body.get("antecedentesGinecoobstetricos"))
                .antecedentesFamiliares((String) body.get("antecedentesFamiliares"))
                .habitosTabaco(toBoolean(body.get("habitosTabaco")))
                .habitosAlcohol(toBoolean(body.get("habitosAlcohol")))
                .habitosSustancias(toBoolean(body.get("habitosSustancias")))
                .habitosDetalles((String) body.get("habitosDetalles"))
                .build();

        hc = historiaClinicaRepository.save(hc);
        return SyncItemResult.builder()
                .clientId(op.getClientId()).success(true).status(201)
                .serverId(hc.getId()).build();
    }

    private SyncItemResult handleUpdate(SyncOperationItem op) throws Exception {
        Long id = extractId(op.getUrl());
        if (id == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("No se pudo extraer ID de la URL").build();
        }

        Optional<HistoriaClinica> opt = historiaClinicaRepository.findById(id);
        if (opt.isEmpty()) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(404)
                    .error("Historia clínica no encontrada: " + id).build();
        }

        HistoriaClinica hc = opt.get();
        Map<String, Object> body = toMap(op.getBody());

        if (body.containsKey("estado")) hc.setEstado((String) body.get("estado"));
        if (body.containsKey("grupoSanguineo")) hc.setGrupoSanguineo((String) body.get("grupoSanguineo"));
        if (body.containsKey("alergiasGenerales")) hc.setAlergiasGenerales((String) body.get("alergiasGenerales"));
        if (body.containsKey("antecedentesPersonales")) hc.setAntecedentesPersonales((String) body.get("antecedentesPersonales"));
        if (body.containsKey("antecedentesQuirurgicos")) hc.setAntecedentesQuirurgicos((String) body.get("antecedentesQuirurgicos"));
        if (body.containsKey("antecedentesFarmacologicos")) hc.setAntecedentesFarmacologicos((String) body.get("antecedentesFarmacologicos"));
        if (body.containsKey("antecedentesTraumaticos")) hc.setAntecedentesTraumaticos((String) body.get("antecedentesTraumaticos"));
        if (body.containsKey("antecedentesGinecoobstetricos")) hc.setAntecedentesGinecoobstetricos((String) body.get("antecedentesGinecoobstetricos"));
        if (body.containsKey("antecedentesFamiliares")) hc.setAntecedentesFamiliares((String) body.get("antecedentesFamiliares"));
        if (body.containsKey("habitosTabaco")) hc.setHabitosTabaco(toBoolean(body.get("habitosTabaco")));
        if (body.containsKey("habitosAlcohol")) hc.setHabitosAlcohol(toBoolean(body.get("habitosAlcohol")));
        if (body.containsKey("habitosSustancias")) hc.setHabitosSustancias(toBoolean(body.get("habitosSustancias")));
        if (body.containsKey("habitosDetalles")) hc.setHabitosDetalles((String) body.get("habitosDetalles"));

        hc = historiaClinicaRepository.save(hc);
        return SyncItemResult.builder()
                .clientId(op.getClientId()).success(true).status(200)
                .serverId(hc.getId()).build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object body) throws Exception {
        if (body instanceof Map) return (Map<String, Object>) body;
        return objectMapper.convertValue(body, Map.class);
    }

    private Long extractId(String url) {
        if (url == null) return null;
        Matcher m = ID_PATTERN.matcher(url);
        return m.find() ? Long.parseLong(m.group(1)) : null;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) { try { return Long.parseLong((String) value); } catch (NumberFormatException e) { return null; } }
        return null;
    }

    private Boolean toBoolean(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return Boolean.parseBoolean((String) value);
        return null;
    }
}
