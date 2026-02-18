/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.PersonalDto;
import com.sesa.salud.dto.PersonalRequestDto;
import com.sesa.salud.service.PersonalService;
import com.sesa.salud.tenant.TenantContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.sesa.salud.dto.LogoResourceDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;

@RestController
@RequestMapping("/personal")
@RequiredArgsConstructor
public class PersonalController {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final PersonalService personalService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public Page<PersonalDto> list(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "schema", required = false) String schema,
            @PageableDefault(size = 20) Pageable pageable) {
        applySchemaOverride(schema);
        return q != null && !q.isBlank() ? personalService.search(q, pageable) : personalService.findAll(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<PersonalDto> get(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Tenant-Schema", required = false) String schema) {
        applySchemaOverride(schema);
        return ResponseEntity.ok(personalService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMINISTRADOR')")
    public ResponseEntity<PersonalDto> create(
            @Valid @RequestBody PersonalRequestDto dto,
            @RequestHeader(value = "X-Tenant-Schema", required = false) String schema) {
        applySchemaOverride(schema);
        return ResponseEntity.status(HttpStatus.CREATED).body(personalService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMINISTRADOR')")
    public ResponseEntity<PersonalDto> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody PersonalRequestDto dto,
            @RequestHeader(value = "X-Tenant-Schema", required = false) String schema) {
        applySchemaOverride(schema);
        return ResponseEntity.ok(personalService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMINISTRADOR')")
    public ResponseEntity<Void> delete(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Tenant-Schema", required = false) String schema) {
        applySchemaOverride(schema);
        personalService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/foto")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMINISTRADOR')")
    public ResponseEntity<Void> uploadFoto(
            @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Tenant-Schema", required = false) String schema) {
        applySchemaOverride(schema);
        personalService.saveFoto(id, file);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/firma")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMINISTRADOR')")
    public ResponseEntity<Void> uploadFirma(
            @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Tenant-Schema", required = false) String schema) {
        applySchemaOverride(schema);
        personalService.saveFirma(id, file);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{id}/foto", produces = { MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE,
            "image/webp" })
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<?> getFoto(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Tenant-Schema", required = false) String schema) {
        applySchemaOverride(schema);
        return personalService.getFotoResource(id)
                .map(logo -> ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.parseMediaType(logo.getContentType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"foto\"")
                        .body(logo.getResource()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/{id}/firma", produces = { MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, "image/webp",
            "image/svg+xml" })
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<?> getFirma(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Tenant-Schema", required = false) String schema) {
        applySchemaOverride(schema);
        return personalService.getFirmaResource(id)
                .map(logo -> ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.parseMediaType(logo.getContentType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"firma\"")
                        .body(logo.getResource()))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Si el usuario es ADMIN y se indica schema, usa ese tenant para la petición
     * (permite al super admin listar/crear/editar/eliminar personal de cualquier
     * empresa).
     */
    private void applySchemaOverride(String schema) {
        if (schema == null || schema.isBlank())
            return;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            return;
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        boolean admin = authorities != null && authorities.stream()
                .anyMatch(a -> ROLE_ADMIN.equals(a.getAuthority()));
        if (admin) {
            TenantContextHolder.setTenantSchema(schema.trim());
        }
    }
}
