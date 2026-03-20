/**
 * Implementación de consulta ADRES/BDUA vía API Apitude (opcional).
 * Requiere API Key de Apitude; si no está configurada, no se realizan llamadas.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.sesa.salud.dto.ConsultaDocumentoDto;
import com.sesa.salud.repository.EpsRepository;
import com.sesa.salud.service.AdresConsultaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdresConsultaServiceImpl implements AdresConsultaService {

    private static final String APITUDE_BASE = "https://apitude.co";
    private static final String REQUEST_PATH = "/api/v1.0/requests/adres-co/";
    private static final int POLL_ATTEMPTS = 15;
    private static final long POLL_DELAY_MS = 2_000;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final EpsRepository epsRepository;

    @Value("${sesa.adres.apitude.api-key:}")
    private String apiKey;

    @Value("${sesa.adres.habilitado:false}")
    private boolean habilitado;

    @Override
    public Optional<ConsultaDocumentoDto> consultarPorDocumento(String tipoDocumento, String documento) {
        if (!habilitado || apiKey == null || apiKey.isBlank()) {
            log.debug("Consulta ADRES deshabilitada o sin API key.");
            return Optional.empty();
        }
        String doc = documento != null ? documento.trim().replaceAll("\\s", "") : "";
        if (doc.isEmpty()) {
            return Optional.empty();
        }
        String docType = mapTipoDocumentoToApitude(tipoDocumento);

        RestTemplate rest = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);

        String body = String.format("{\"document_type\":\"%s\",\"document_number\":\"%s\"}", docType, doc);
        ResponseEntity<JsonNode> createResp = rest.exchange(
                APITUDE_BASE + REQUEST_PATH,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                JsonNode.class
        );

        JsonNode createJson = createResp.getBody();
        if (createJson == null || !createJson.has("url")) {
            log.warn("Apitude ADRES: respuesta POST sin url.");
            return Optional.empty();
        }
        String pollUrl = createJson.get("url").asText();
        if (!pollUrl.startsWith("http")) {
            pollUrl = APITUDE_BASE + (pollUrl.startsWith("/") ? pollUrl : "/" + pollUrl);
        }

        for (int i = 0; i < POLL_ATTEMPTS; i++) {
            try {
                Thread.sleep(POLL_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
            ResponseEntity<JsonNode> getResp = rest.exchange(pollUrl, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
            JsonNode getJson = getResp.getBody();
            if (getJson == null) continue;
            JsonNode result = getJson.get("result");
            if (result == null) continue;
            int status = result.has("status") ? result.path("status").asInt(0) : 0;
            if (status == 200 && result.has("data")) {
                return mapApitudeDataToDto(result.get("data"), tipoDocumento, documento);
            }
            if (status == 404) {
                log.debug("Apitude ADRES: sin datos para documento {}", documento);
                return Optional.empty();
            }
        }
        log.warn("Apitude ADRES: timeout esperando resultado para documento {}", documento);
        return Optional.empty();
    }

    private String mapTipoDocumentoToApitude(String tipo) {
        if (tipo == null || tipo.isBlank()) return "cedula";
        return switch (tipo.toUpperCase()) {
            case "CC" -> "cedula";
            case "CE" -> "cedula_extranjeria";
            case "TI" -> "tarjeta_identidad";
            case "PA" -> "pasaporte";
            default -> "cedula";
        };
    }

    private Optional<ConsultaDocumentoDto> mapApitudeDataToDto(JsonNode data, String tipoDocumento, String documento) {
        if (data == null) return Optional.empty();
        String nombres = data.has("nombres") ? data.get("nombres").asText("") : "";
        String apellidos = data.has("apellidos") ? data.get("apellidos").asText("") : "";
        String fechaNac = data.has("fecha_de_nacimiento") ? data.get("fecha_de_nacimiento").asText(null) : null;
        LocalDate fechaNacimiento = null;
        if (fechaNac != null && !fechaNac.isBlank()) {
            try {
                fechaNacimiento = LocalDate.parse(fechaNac, DATE_FMT);
            } catch (Exception ignored) { }
        }
        String municipio = data.has("municipio") ? data.get("municipio").asText(null) : null;
        String departamento = data.has("departamento") ? data.get("departamento").asText(null) : null;

        String regimen = null;
        String tipoUsuario = null;
        String epsNombre = null;
        String estadoAfiliacion = null;
        JsonNode est = data.get("estado_afiliacion");
        if (est != null) {
            estadoAfiliacion = est.has("estado") ? est.get("estado").asText(null) : null;
            epsNombre = est.has("entidad") ? est.get("entidad").asText(null) : null;
            String reg = est.has("regimen") ? est.get("regimen").asText(null) : null;
            if (reg != null) {
                regimen = "CONTRIBUTIVO".equalsIgnoreCase(reg) ? "CONTRIBUTIVO"
                        : "SUBSIDIADO".equalsIgnoreCase(reg) ? "SUBSIDIADO" : reg;
            }
            String tipoAfil = est.has("tipo_de_afiliado") ? est.get("tipo_de_afiliado").asText(null) : null;
            if (tipoAfil != null) {
                tipoUsuario = "COTIZANTE".equalsIgnoreCase(tipoAfil) ? "AFILIADO_COTIZANTE"
                        : "BENEFICIARIO".equalsIgnoreCase(tipoAfil) ? "AFILIADO_BENEFICIARIO" : tipoAfil;
            }
        }

        Long epsId = (epsNombre != null && !epsNombre.isBlank())
                ? epsRepository.findFirstByNombreContainingIgnoreCase(epsNombre).map(eps -> eps.getId()).orElse(null)
                : null;

        ConsultaDocumentoDto dto = ConsultaDocumentoDto.builder()
                .tipoDocumento(tipoDocumento != null ? tipoDocumento : "CC")
                .documento(documento)
                .nombres(nombres)
                .apellidos(apellidos)
                .fechaNacimiento(fechaNacimiento)
                .sexo(null)
                .municipioResidencia(municipio)
                .departamentoResidencia(departamento)
                .regimenAfiliacion(regimen)
                .tipoUsuario(tipoUsuario)
                .epsNombre(epsNombre)
                .epsId(epsId)
                .estadoAfiliacion(estadoAfiliacion)
                .build();
        return Optional.of(dto);
    }
}
