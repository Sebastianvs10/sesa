/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesa.salud.dto.SyncBatchRequest.SyncOperationItem;
import com.sesa.salud.dto.SyncBatchResponse.SyncItemResult;
import com.sesa.salud.entity.Atencion;
import com.sesa.salud.entity.HistoriaClinica;
import com.sesa.salud.entity.Personal;
import com.sesa.salud.repository.AtencionRepository;
import com.sesa.salud.repository.HistoriaClinicaRepository;
import com.sesa.salud.repository.PersonalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handler de sincronización offline para la entidad Atenciones.
 * Soporta POST (crear).
 */
@Component("atenciones")
@RequiredArgsConstructor
@Slf4j
public class AtencionSyncHandler implements SyncEntityHandler {

    private final AtencionRepository atencionRepository;
    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final PersonalRepository personalRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern ID_PATTERN = Pattern.compile("/atenciones/(\\d+)");

    @Override
    public SyncItemResult handle(SyncOperationItem op, String userEmail) {
        String method = op.getMethod().toUpperCase();
        try {
            return switch (method) {
                case "POST" -> handleCreate(op);
                default -> SyncItemResult.builder()
                        .clientId(op.getClientId()).success(false).status(405)
                        .error("Método no soportado: " + method).build();
            };
        } catch (Exception e) {
            log.error("Error en AtencionSyncHandler: {}", e.getMessage(), e);
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(500)
                    .error("Error procesando atencion: " + e.getMessage()).build();
        }
    }

    private SyncItemResult handleCreate(SyncOperationItem op) throws Exception {
        Map<String, Object> body = toMap(op.getBody());
        
        Long historiaId = toLong(body.get("historiaId"));
        Long profesionalId = toLong(body.get("profesionalId"));
        
        if (historiaId == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("historiaId es obligatorio").build();
        }
        
        if (profesionalId == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("profesionalId es obligatorio").build();
        }
        
        HistoriaClinica historiaClinica = historiaClinicaRepository.findById(historiaId)
                .orElseThrow(() -> new RuntimeException("Historia clínica no encontrada: " + historiaId));
        
        Personal profesional = personalRepository.findById(profesionalId)
                .orElseThrow(() -> new RuntimeException("Personal no encontrado: " + profesionalId));
        
        String fechaAtencionStr = (String) body.get("fechaAtencion");
        Instant fechaAtencion = fechaAtencionStr != null ? Instant.parse(fechaAtencionStr) : Instant.now();
        
        Atencion a = Atencion.builder()
                .historiaClinica(historiaClinica)
                .profesional(profesional)
                .fechaAtencion(fechaAtencion)
                .motivoConsulta((String) body.get("motivoConsulta"))
                .enfermedadActual((String) body.get("enfermedadActual"))
                .versionEnfermedad((String) body.get("versionEnfermedad"))
                .sintomasAsociados((String) body.get("sintomasAsociados"))
                .factoresMejoran((String) body.get("factoresMejoran"))
                .factoresEmpeoran((String) body.get("factoresEmpeoran"))
                .revisionSistemas((String) body.get("revisionSistemas"))
                .presionArterial((String) body.get("presionArterial"))
                .frecuenciaCardiaca((String) body.get("frecuenciaCardiaca"))
                .frecuenciaRespiratoria((String) body.get("frecuenciaRespiratoria"))
                .temperatura((String) body.get("temperatura"))
                .peso((String) body.get("peso"))
                .talla((String) body.get("talla"))
                .imc((String) body.get("imc"))
                .evaluacionGeneral((String) body.get("evaluacionGeneral"))
                .hallazgos((String) body.get("hallazgos"))
                .diagnostico((String) body.get("diagnostico"))
                .codigoCie10((String) body.get("codigoCie10"))
                .planTratamiento((String) body.get("planTratamiento"))
                .tratamientoFarmacologico((String) body.get("tratamientoFarmacologico"))
                .ordenesMedicas((String) body.get("ordenesMedicas"))
                .examenesSolicitados((String) body.get("examenesSolicitados"))
                .incapacidad((String) body.get("incapacidad"))
                .recomendaciones((String) body.get("recomendaciones"))
                .build();
        
        a = atencionRepository.save(a);
        
        return SyncItemResult.builder()
                .clientId(op.getClientId()).success(true).status(201)
                .serverId(a.getId()).build();
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
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
