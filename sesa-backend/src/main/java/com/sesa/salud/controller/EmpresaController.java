/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.EmpresaCreateRequest;
import com.sesa.salud.dto.EmpresaDto;
import com.sesa.salud.service.EmpresaService;
import com.sesa.salud.tenant.TenantContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/empresas")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaService empresaService;

    /**
     * Datos de la empresa del tenant actual (para mostrar nombre y saber si hay
     * logo).
     */
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmpresaDto> getCurrent() {
        String schema = TenantContextHolder.getTenantSchema();
        return empresaService.findBySchemaName(schema)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Módulos y submódulos contratados por la empresa del tenant actual. */
    @GetMapping("/current/modulos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getCurrentModulos() {
        String schema = TenantContextHolder.getTenantSchema();
        Optional<EmpresaDto> opt = empresaService.findBySchemaName(schema);
        if (opt.isEmpty())
            return ResponseEntity.notFound().build();
        EmpresaDto dto = opt.get();
        EmpresaDto full = empresaService.findById(dto.getId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("moduloCodigos", full.getModuloCodigos() != null ? full.getModuloCodigos() : List.of());
        result.put("submoduloCodigos", full.getSubmoduloCodigos() != null ? full.getSubmoduloCodigos() : List.of());
        return ResponseEntity.ok(result);
    }

    /**
     * Logo de la empresa del tenant actual. El frontend puede pedirlo con
     * responseType blob y crear object URL.
     */
    @GetMapping(value = "/logo", produces = { MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, "image/webp",
            "image/svg+xml" })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getLogo() {
        String schema = TenantContextHolder.getTenantSchema();
        return empresaService.getLogoResource(schema)
                .map(logo -> ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.parseMediaType(logo.getContentType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"logo\"")
                        .body(logo.getResource()))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Subir logo de la empresa del tenant actual. Solo ADMIN del tenant. */
    @PostMapping("/logo")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMINISTRADOR')")
    public ResponseEntity<Map<String, String>> uploadLogo(@RequestParam("file") MultipartFile file) {
        String schema = TenantContextHolder.getTenantSchema();
        if (TenantContextHolder.PUBLIC.equals(schema)) {
            return ResponseEntity.badRequest().build();
        }
        String uuid = empresaService.saveLogo(schema, file);
        return ResponseEntity.ok(Map.of("uuid", uuid, "url", "/archivos/" + uuid));
    }

    /**
     * Subir logo de una empresa específica por ID.
     * Permite al SUPERADMINISTRADOR gestionar logos de cualquier empresa
     * sin importar su tenant (schema "public").
     */
    @PostMapping("/{id}/logo")
    @PreAuthorize("hasRole('SUPERADMINISTRADOR') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadLogoById(
            @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file) {
        String uuid = empresaService.saveLogoById(id, file);
        return ResponseEntity.ok(Map.of("uuid", uuid, "url", "/archivos/" + uuid));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMINISTRADOR')")
    public Page<EmpresaDto> list(@PageableDefault(size = 20) Pageable pageable) {
        return empresaService.findAll(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMINISTRADOR')")
    public ResponseEntity<EmpresaDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(empresaService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPERADMINISTRADOR')")
    public ResponseEntity<EmpresaDto> create(@Valid @RequestBody EmpresaCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(empresaService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMINISTRADOR')")
    public ResponseEntity<EmpresaDto> update(@PathVariable("id") Long id, @Valid @RequestBody EmpresaCreateRequest request) {
        return ResponseEntity.ok(empresaService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        empresaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
