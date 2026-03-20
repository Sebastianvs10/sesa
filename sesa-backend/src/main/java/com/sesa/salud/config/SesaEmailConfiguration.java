/**
 * Beans de correo: propiedades, RestTemplate para Resend.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(SesaEmailProperties.class)
public class SesaEmailConfiguration {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
