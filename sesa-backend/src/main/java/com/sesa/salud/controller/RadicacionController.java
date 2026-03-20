/**
 * API de radicación de facturas ante EPS.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.dto.RadicacionDto;
import com.sesa.salud.dto.RadicacionRequestDto;
import com.sesa.salud.service.RadicacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/radicaciones")
@RequiredArgsConstructor
public class RadicacionController {

    private final RadicacionService radicacionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public Page<RadicacionDto> listAll(
            @RequestParam(value = "estado", required = false) String estado,
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(value = "facturaId", required = false) Long facturaId,
            @PageableDefault(size = 20, sort = "fechaRadicacion") Pageable pageable) {
        Instant desdeInst = desde != null ? desde.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant hastaInst = hasta != null ? hasta.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        return radicacionService.findAllFiltered(estado, desdeInst, hastaInst, facturaId, pageable);
    }

    @GetMapping("/factura/{facturaId}")
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public List<RadicacionDto> listByFactura(@PathVariable Long facturaId) {
        return radicacionService.findByFacturaId(facturaId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<RadicacionDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(radicacionService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<RadicacionDto> create(@Valid @RequestBody RadicacionRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(radicacionService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<RadicacionDto> update(@PathVariable Long id, @Valid @RequestBody RadicacionRequestDto dto) {
        return ResponseEntity.ok(radicacionService.update(id, dto));
    }
}
