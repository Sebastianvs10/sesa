/**
 * S12: Implementación del servicio de API Keys para integradores.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.ApiKeyCreateDto;
import com.sesa.salud.dto.ApiKeyResponseDto;
import com.sesa.salud.entity.ApiKey;
import com.sesa.salud.repository.ApiKeyRepository;
import com.sesa.salud.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int RAW_KEY_BYTES = 32;
    private static final String INDEX_ALG = "SHA-256";

    @Override
    @Transactional(readOnly = true)
    public List<ApiKeyResponseDto> listar() {
        return apiKeyRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponseDto).toList();
    }

    @Override
    @Transactional
    public ApiKeyCreateDto crear(String nombreIntegrador, String permisos) {
        String rawKey = generarClaveSegura();
        String index = computeIndex(rawKey);
        String hash = passwordEncoder.encode(rawKey);
        ApiKey entity = ApiKey.builder()
                .nombreIntegrador(nombreIntegrador != null ? nombreIntegrador.trim() : "Integrador")
                .apiKeyHash(hash)
                .apiKeyIndex(index)
                .permisos(permisos != null ? permisos.trim() : "LABORATORIO")
                .activo(true)
                .build();
        entity = apiKeyRepository.save(entity);
        log.info("API Key creada — id:{} nombre:{}", entity.getId(), entity.getNombreIntegrador());
        return ApiKeyCreateDto.builder()
                .id(entity.getId())
                .nombreIntegrador(entity.getNombreIntegrador())
                .apiKeyRaw(rawKey)
                .permisos(entity.getPermisos())
                .build();
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        apiKeyRepository.findById(id).ifPresent(key -> {
            key.setActivo(false);
            apiKeyRepository.save(key);
            log.info("API Key desactivada — id:{}", id);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ApiKey validar(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) return null;
        String index = computeIndex(rawKey.trim());
        Optional<ApiKey> opt = apiKeyRepository.findByApiKeyIndexAndActivoTrue(index);
        if (opt.isEmpty()) return null;
        ApiKey key = opt.get();
        if (passwordEncoder.matches(rawKey.trim(), key.getApiKeyHash())) return key;
        return null;
    }

    private String generarClaveSegura() {
        SecureRandom r = new SecureRandom();
        byte[] bytes = new byte[RAW_KEY_BYTES];
        r.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String computeIndex(String rawKey) {
        try {
            MessageDigest md = MessageDigest.getInstance(INDEX_ALG);
            byte[] digest = md.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible", e);
        }
    }

    private ApiKeyResponseDto toResponseDto(ApiKey e) {
        return ApiKeyResponseDto.builder()
                .id(e.getId())
                .nombreIntegrador(e.getNombreIntegrador())
                .permisos(e.getPermisos())
                .activo(e.getActivo())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
