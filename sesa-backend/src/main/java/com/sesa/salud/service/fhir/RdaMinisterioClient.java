/**
 * Cliente HTTP para la plataforma IHCE del Ministerio de Salud
 * Resolución 1888 de 2025 — Anexo Técnico: API Gateway con API Key
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.fhir;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Envía Bundles FHIR R4 a la plataforma de Interoperabilidad
 * de Historia Clínica Electrónica (IHCE) del Ministerio de Salud.
 *
 * Endpoint base: https://fhir.minsalud.gov.co  (prod)
 * Auth: API Key en header X-API-Key (según Anexo Técnico Res.1888/2025)
 * Cifrado: TLS 1.3 (requerido por la norma)
 */
@Slf4j
@Component
public class RdaMinisterioClient {

    @Value("${sesa.fhir.ministerio.base-url:https://fhir.minsalud.gov.co}")
    private String baseUrl;

    @Value("${sesa.fhir.ministerio.api-key:}")
    private String apiKey;

    @Value("${sesa.fhir.ministerio.habilitado:false}")
    private boolean habilitado;

    private final RestTemplate restTemplate;

    public RdaMinisterioClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Envía un Bundle FHIR (JSON) al servidor FHIR del Ministerio.
     * El endpoint es POST /Bundle conforme a FHIR RESTful API.
     *
     * @param bundleJson JSON del Bundle FHIR R4
     * @return ID asignado por el Ministerio, o null si no habilitado
     */
    public String enviarBundle(String bundleJson) {
        if (!habilitado) {
            log.info("Envío al Ministerio deshabilitado (modo desarrollo). Bundle NO enviado.");
            return "DEV-" + System.currentTimeMillis();
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "API Key del Ministerio no configurada. Configure sesa.fhir.ministerio.api-key");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/fhir+json; charset=UTF-8"));
            headers.set("X-API-Key", apiKey);
            headers.set("Accept", "application/fhir+json");

            HttpEntity<String> request = new HttpEntity<>(bundleJson, headers);
            String endpoint = baseUrl + "/Bundle";

            log.info("Enviando RDA al Ministerio: {}", endpoint);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    endpoint, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String locationHeader = response.getHeaders()
                        .getFirst(HttpHeaders.LOCATION);
                log.info("RDA enviado exitosamente. Location: {}", locationHeader);
                return extraerIdDeLocation(locationHeader);
            } else {
                log.error("Error del Ministerio: HTTP {} - {}",
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("Error HTTP " + response.getStatusCode()
                        + " al enviar RDA al Ministerio");
            }

        } catch (Exception e) {
            log.error("Error enviando RDA al Ministerio: {}", e.getMessage(), e);
            throw new RuntimeException("Error de comunicación con IHCE Ministerio: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Consulta el estado de un Bundle previamente enviado.
     */
    public Map<String, Object> consultarEstado(String idMinisterio) {
        if (!habilitado) return Map.of("estado", "DEV", "id", idMinisterio);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", apiKey);
            headers.set("Accept", "application/fhir+json");

            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/Bundle/" + idMinisterio,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class);

            return response.getBody() != null ? response.getBody() : Map.of();
        } catch (Exception e) {
            log.warn("Error consultando estado RDA {}: {}", idMinisterio, e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    private String extraerIdDeLocation(String location) {
        if (location == null) return null;
        String[] partes = location.split("/");
        return partes.length > 0 ? partes[partes.length - 1] : location;
    }
}
