/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.dto.FacturaDto;
import com.sesa.salud.dto.FacturaRequestDto;
import com.sesa.salud.service.FacturaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/facturas")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturaService facturaService;

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','SUPERADMINISTRADOR')")
    public List<FacturaDto> listByPaciente(@PathVariable("pacienteId") Long pacienteId,
                                           @PageableDefault(size = 20) Pageable pageable) {
        return facturaService.findByPacienteId(pacienteId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<FacturaDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(facturaService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<FacturaDto> create(@Valid @RequestBody FacturaRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facturaService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<FacturaDto> update(@PathVariable("id") Long id, @Valid @RequestBody FacturaRequestDto dto) {
        return ResponseEntity.ok(facturaService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        facturaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/rips", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','SUPERADMINISTRADOR')")
    public ResponseEntity<String> exportRips(
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        Instant d = desde.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant h = hasta.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        String csv = facturaService.exportRipsCsv(d, h);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
