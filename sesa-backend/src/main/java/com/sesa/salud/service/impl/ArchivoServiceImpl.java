/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.entity.master.ArchivoAlmacenamiento;
import com.sesa.salud.repository.master.ArchivoAlmacenamientoRepository;
import com.sesa.salud.service.ArchivoService;
import com.sesa.salud.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArchivoServiceImpl implements ArchivoService {

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024L; // 5 MB
    private static final java.util.Set<String> ALLOWED_TYPES = java.util.Set.of(
            "image/png", "image/jpeg", "image/webp", "image/svg+xml",
            "image/gif", "application/pdf"
    );

    private final ArchivoAlmacenamientoRepository archivoRepository;

    @Override
    @Transactional
    public String guardar(MultipartFile file, String entidad, String entidadId,
                          String schemaTenant, boolean accesoPublico) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo permitido (5 MB)");
        }

        String contentType = resolveContentType(file);
        String nombreOriginal = file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "archivo";

        try {
            byte[] bytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            return persistir(base64, contentType, nombreOriginal,
                    (int) file.getSize(), entidad, entidadId, schemaTenant, accesoPublico);
        } catch (IOException e) {
            log.error("Error leyendo bytes del archivo: {}", e.getMessage());
            throw new RuntimeException("No se pudo procesar el archivo: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public String guardarBase64(String base64, String contentType, String nombreOriginal,
                                String entidad, String entidadId,
                                String schemaTenant, boolean accesoPublico) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalArgumentException("El contenido base64 es obligatorio");
        }
        // Quitar prefijo data:URI si lo tiene (p. ej. "data:image/png;base64,...")
        String datos = base64.contains(",") ? base64.substring(base64.indexOf(',') + 1) : base64;
        int tamanio = Base64.getDecoder().decode(datos).length;
        return persistir(datos, contentType, nombreOriginal, tamanio,
                entidad, entidadId, schemaTenant, accesoPublico);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ArchivoRecurso> obtener(String uuid) {
        // Los archivos siempre están en el schema público (master)
        TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
        return archivoRepository.findByUuid(uuid)
                .map(a -> {
                    byte[] bytes = Base64.getDecoder().decode(a.getDatos());
                    return new ArchivoRecurso(
                            bytes,
                            a.getContentType(),
                            a.getNombreOriginal(),
                            a.isAccesoPublico(),
                            a.getSchemaTenant()
                    );
                });
    }

    @Override
    @Transactional
    public void eliminar(String uuid) {
        TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
        archivoRepository.deleteById(uuid);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String persistir(String base64, String contentType, String nombreOriginal,
                              int tamanio, String entidad, String entidadId,
                              String schemaTenant, boolean accesoPublico) {
        TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
        String uuid = UUID.randomUUID().toString();
        ArchivoAlmacenamiento archivo = ArchivoAlmacenamiento.builder()
                .uuid(uuid)
                .datos(base64)
                .contentType(contentType)
                .nombreOriginal(nombreOriginal)
                .tamanioBytes(tamanio)
                .entidad(entidad)
                .entidadId(entidadId)
                .schemaTenant(schemaTenant != null ? schemaTenant : "public")
                .accesoPublico(accesoPublico)
                .build();
        archivoRepository.save(archivo);
        log.debug("Archivo guardado: uuid={} entidad={} tamanio={}B", uuid, entidad, tamanio);
        return uuid;
    }

    private String resolveContentType(MultipartFile file) {
        String ct = file.getContentType();
        if (ct != null && ALLOWED_TYPES.contains(ct)) return ct;
        // Inferir por extensión si el MIME no es de confianza
        String nombre = file.getOriginalFilename();
        if (nombre != null) {
            int dot = nombre.lastIndexOf('.');
            if (dot >= 0) {
                return switch (nombre.substring(dot + 1).toLowerCase()) {
                    case "png"  -> "image/png";
                    case "jpg", "jpeg" -> "image/jpeg";
                    case "webp" -> "image/webp";
                    case "svg"  -> "image/svg+xml";
                    case "gif"  -> "image/gif";
                    case "pdf"  -> "application/pdf";
                    default     -> "application/octet-stream";
                };
            }
        }
        return ct != null ? ct : "application/octet-stream";
    }
}
