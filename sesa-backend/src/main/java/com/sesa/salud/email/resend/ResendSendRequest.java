/**
 * Payload JSON para POST /emails de Resend.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.email.resend;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResendSendRequest(
        String from,
        List<String> to,
        String subject,
        String html,
        @JsonProperty("reply_to") String replyTo
) {}
