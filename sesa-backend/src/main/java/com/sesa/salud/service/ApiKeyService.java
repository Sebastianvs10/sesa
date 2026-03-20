/**
 * S12: Servicio de API Keys para integradores.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.ApiKeyCreateDto;
import com.sesa.salud.dto.ApiKeyResponseDto;
import com.sesa.salud.entity.ApiKey;

import java.util.List;

public interface ApiKeyService {
    List<ApiKeyResponseDto> listar();
    /** Crea una nueva API Key; la clave en texto solo se devuelve en esta respuesta. */
    ApiKeyCreateDto crear(String nombreIntegrador, String permisos);
    void desactivar(Long id);
    /** Valida la clave y devuelve la entidad si es válida y activa (para uso del filtro). */
    ApiKey validar(String apiKeyHash);
}
