/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.io.Resource;

/**
 * Recurso de imagen (logo, foto, firma) con su tipo de contenido para servir
 * correctamente.
 */
@Getter
@AllArgsConstructor
public class LogoResourceDto {
    private final Resource resource;
    private final String contentType;
}
