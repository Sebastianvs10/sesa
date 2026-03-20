/**
 * S9: REST de glosas (rechazos de factura) y adjuntos.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.dto.GlosaDto;
import com.sesa.salud.dto.GlosaRequestDto;
import com.sesa.salud.security.JwtPrincipal;
import com.sesa.salud.service.GlosaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/glosas")
@RequiredArgsConstructor
public class GlosaController {

    private final GlosaService glosaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','FACTURACION','RECEPCIONISTA')")
    public List<GlosaDto> list(
            @RequestParam(required = false) Long facturaId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Instant desde,
            @RequestParam(required = false) Instant hasta) {
        return glosaService.list(estado, desde, hasta, facturaId);
    }

    @GetMapping("/factura/{facturaId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','FACTURACION','RECEPCIONISTA')")
    public List<GlosaDto> listByFactura(@PathVariable Long facturaId) {
        return glosaService.findByFacturaId(facturaId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','FACTURACION','RECEPCIONISTA')")
    public ResponseEntity<GlosaDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(glosaService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','FACTURACION','RECEPCIONISTA')")
    public ResponseEntity<GlosaDto> create(@Valid @RequestBody GlosaRequestDto dto, Authentication auth) {
        Long userId = auth != null && auth.getPrincipal() instanceof JwtPrincipal jp ? jp.userId() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(glosaService.create(dto, userId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','FACTURACION','RECEPCIONISTA')")
    public ResponseEntity<GlosaDto> update(@PathVariable Long id, @Valid @RequestBody GlosaRequestDto dto) {
        return ResponseEntity.ok(glosaService.update(id, dto));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','FACTURACION','RECEPCIONISTA')")
    public ResponseEntity<GlosaDto> cambiarEstado(@PathVariable Long id, @RequestParam String estado) {
        return ResponseEntity.ok(glosaService.cambiarEstado(id, estado));
    }

    @PostMapping("/{id}/adjuntos")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','FACTURACION','RECEPCIONISTA')")
    public ResponseEntity<GlosaDto> uploadAdjunto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(glosaService.uploadAdjunto(id, file));
    }
}
