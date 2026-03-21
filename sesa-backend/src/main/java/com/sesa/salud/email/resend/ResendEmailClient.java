/**
 * Cliente HTTP mínimo para la API de Resend (envío de correos).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.email.resend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesa.salud.config.SesaEmailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResendEmailClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SesaEmailProperties emailProperties;

    /**
     * Envía un correo HTML. No lanza excepción al caller: registra error y devuelve vacío si falla.
     */
    public Optional<String> sendHtml(String to, String subject, String html) {
        if (!emailProperties.isResendConfigured()) {
            log.warn(
                    "Correo no enviado (sesa.email.enabled=false o RESEND_API_KEY vacía). Asunto={}. Defina RESEND_API_KEY y, en prod, SESA_EMAIL_ENABLED=true.",
                    subject);
            return Optional.empty();
        }
        if (to == null || to.isBlank()) {
            log.warn("Correo omitido: destinatario vacío");
            return Optional.empty();
        }

        String reply = emailProperties.getReplyTo() != null && !emailProperties.getReplyTo().isBlank()
                ? emailProperties.getReplyTo().trim()
                : null;
        ResendSendRequest body = new ResendSendRequest(
                emailProperties.getFrom().trim(),
                List.of(to.trim().toLowerCase()),
                subject,
                html,
                reply
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(emailProperties.getResendApiKey().trim());
            String url = emailProperties.getResendApiBase().replaceAll("/$", "") + "/emails";
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Correo enviado vía Resend: destino=**** asunto={}", subject);
                return Optional.ofNullable(response.getBody());
            }
            log.error("Resend respuesta no exitosa: status={} body={}", response.getStatusCode(), sanitizeForLog(response.getBody()));
        } catch (RestClientResponseException e) {
            log.error(
                    "Resend error HTTP {}: {}",
                    e.getStatusCode().value(),
                    sanitizeForLog(e.getResponseBodyAsString()),
                    e);
        } catch (Exception e) {
            log.error("Resend error al enviar: {}", e.getMessage(), e);
        }
        return Optional.empty();
    }

    private static String sanitizeForLog(String s) {
        if (s == null || s.length() < 200) return s;
        return s.substring(0, 200) + "…";
    }
}
