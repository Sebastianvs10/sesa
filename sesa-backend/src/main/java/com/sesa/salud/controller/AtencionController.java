/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.dto.AtencionDto;
import com.sesa.salud.dto.AtencionRequestDto;
import com.sesa.salud.dto.ReconciliacionAtencionDto;
import com.sesa.salud.dto.ReconciliacionAtencionRequestDto;
import com.sesa.salud.service.AtencionService;
import com.sesa.salud.service.ReconciliacionService;
import com.sesa.salud.service.HistoriaClinicaPdfService;
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

@RestController
@RequestMapping("/atenciones")
@RequiredArgsConstructor
public class AtencionController {

    private final AtencionService atencionService;
    private final ReconciliacionService reconciliacionService;
    private final HistoriaClinicaPdfService historiaClinicaPdfService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','SUPERADMINISTRADOR')")
    public Page<AtencionDto> listByHistoria(@RequestParam("historiaId") Long historiaId,
                                            @PageableDefault(size = 20) Pageable pageable) {
        return atencionService.findByHistoriaId(historiaId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<AtencionDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(atencionService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<AtencionDto> create(@Valid @RequestBody AtencionRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(atencionService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<AtencionDto> update(@PathVariable("id") Long id,
                                              @Valid @RequestBody AtencionRequestDto dto) {
        return ResponseEntity.ok(atencionService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        atencionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** S5: Reconciliación de medicamentos y alergias. */
    @GetMapping("/{id}/reconciliacion")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<ReconciliacionAtencionDto> getReconciliacion(@PathVariable("id") Long id) {
        return ResponseEntity.ok(reconciliacionService.getByAtencionId(id));
    }

    @PostMapping("/{id}/reconciliacion")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<ReconciliacionAtencionDto> guardarReconciliacion(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReconciliacionAtencionRequestDto request) {
        return ResponseEntity.ok(reconciliacionService.guardar(id, request));
    }

    /** S6: Guardar referencia (motivo, nivel, datos para PDF). */
    @PostMapping("/{id}/referencia")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<AtencionDto> guardarReferencia(
            @PathVariable("id") Long id,
            @RequestBody(required = false) com.sesa.salud.dto.AltaReferenciaRequestDto request) {
        return ResponseEntity.ok(atencionService.guardarReferencia(id, request));
    }

    /** S6: Descargar PDF de referencia. */
    @GetMapping("/{id}/referencia/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<byte[]> pdfReferencia(@PathVariable("id") Long id) {
        byte[] pdf = historiaClinicaPdfService.generarPdfReferenciaAtencion(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"referencia-" + id + ".pdf\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(pdf.length))
                .body(pdf);
    }
}
