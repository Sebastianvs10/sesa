/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.UrgenciaDashboardDto;
import com.sesa.salud.dto.UrgenciaRegistroDto;
import com.sesa.salud.dto.UrgenciaRegistroRequestDto;
import com.sesa.salud.dto.UrgenciaReporteCumplimientoDto;
import com.sesa.salud.dto.UrgenciaTriagePatchDto;
import com.sesa.salud.dto.AltaReferenciaRequestDto;
import com.sesa.salud.service.UrgenciaRegistroService;
import com.sesa.salud.dto.SignosVitalesUrgenciaDto;
import com.sesa.salud.dto.SignosVitalesUrgenciaRequestDto;
import com.sesa.salud.service.SignosVitalesUrgenciaService;
import com.sesa.salud.service.HistoriaClinicaPdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/urgencias")
@RequiredArgsConstructor
public class UrgenciaRegistroController {

    private final UrgenciaRegistroService urgenciaRegistroService;
    private final SignosVitalesUrgenciaService signosVitalesUrgenciaService;
    private final HistoriaClinicaPdfService historiaClinicaPdfService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','COORDINADOR_MEDICO','JEFE_ENFERMERIA','ENFERMERO','AUXILIAR_ENFERMERIA','RECEPCIONISTA')")
    public Object list(
            @RequestParam(value = "estado", required = false) String estado,
            @PageableDefault(size = 50) Pageable pageable) {
        if (estado != null && !estado.isBlank()) {
            return urgenciaRegistroService.findByEstado(estado, pageable);
        }
        return urgenciaRegistroService.findAll(pageable);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','COORDINADOR_MEDICO','JEFE_ENFERMERIA','ENFERMERO','AUXILIAR_ENFERMERIA','RECEPCIONISTA')")
    public ResponseEntity<UrgenciaDashboardDto> dashboard() {
        return ResponseEntity.ok(urgenciaRegistroService.getDashboard());
    }

    @GetMapping("/reporte-cumplimiento")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','COORDINADOR_MEDICO','JEFE_ENFERMERIA')")
    public ResponseEntity<UrgenciaReporteCumplimientoDto> reporteCumplimiento(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(urgenciaRegistroService.getReporteCumplimiento(desde, hasta));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','COORDINADOR_MEDICO','JEFE_ENFERMERIA','ENFERMERO','AUXILIAR_ENFERMERIA','RECEPCIONISTA')")
    public ResponseEntity<UrgenciaRegistroDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(urgenciaRegistroService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','COORDINADOR_MEDICO','JEFE_ENFERMERIA','ENFERMERO','AUXILIAR_ENFERMERIA','RECEPCIONISTA')")
    public ResponseEntity<UrgenciaRegistroDto> create(@Valid @RequestBody UrgenciaRegistroRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(urgenciaRegistroService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','COORDINADOR_MEDICO','JEFE_ENFERMERIA','ENFERMERO')")
    public ResponseEntity<UrgenciaRegistroDto> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody UrgenciaRegistroRequestDto dto) {
        return ResponseEntity.ok(urgenciaRegistroService.update(id, dto));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','COORDINADOR_MEDICO','JEFE_ENFERMERIA','ENFERMERO')")
    public ResponseEntity<UrgenciaRegistroDto> cambiarEstado(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        String nuevoEstado = body.get("estado");
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(urgenciaRegistroService.cambiarEstado(id, nuevoEstado));
    }

    @PatchMapping("/{id}/triage")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','COORDINADOR_MEDICO','JEFE_ENFERMERIA','ENFERMERO')")
    public ResponseEntity<UrgenciaRegistroDto> updateTriage(
            @PathVariable("id") Long id,
            @RequestBody UrgenciaTriagePatchDto dto) {
        return ResponseEntity.ok(urgenciaRegistroService.updateTriage(id, dto));
    }

    @GetMapping("/{id}/signos-vitales")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','COORDINADOR_MEDICO','JEFE_ENFERMERIA','ENFERMERO','AUXILIAR_ENFERMERIA')")
    public ResponseEntity<List<SignosVitalesUrgenciaDto>> listSignosVitales(@PathVariable("id") Long id) {
        return ResponseEntity.ok(signosVitalesUrgenciaService.findByUrgenciaRegistroId(id));
    }

    @PostMapping("/{id}/signos-vitales")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','COORDINADOR_MEDICO','JEFE_ENFERMERIA','ENFERMERO','AUXILIAR_ENFERMERIA')")
    public ResponseEntity<SignosVitalesUrgenciaDto> createSignosVitales(
            @PathVariable("id") Long id,
            @Valid @RequestBody SignosVitalesUrgenciaRequestDto dto) {
        dto.setUrgenciaRegistroId(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(signosVitalesUrgenciaService.create(dto));
    }

    /** S6: Dar alta al registro de urgencia (estado ALTA + datos para PDF). */
    @PostMapping("/{id}/alta")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','COORDINADOR_MEDICO','JEFE_ENFERMERIA','ENFERMERO')")
    public ResponseEntity<UrgenciaRegistroDto> darAlta(
            @PathVariable("id") Long id,
            @RequestBody(required = false) AltaReferenciaRequestDto request) {
        return ResponseEntity.ok(urgenciaRegistroService.darAlta(id, request));
    }

    /** S6: Descargar PDF de resumen de alta de urgencias. */
    @GetMapping("/{id}/alta/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','COORDINADOR_MEDICO','JEFE_ENFERMERIA','ENFERMERO')")
    public ResponseEntity<byte[]> pdfAlta(@PathVariable("id") Long id) {
        byte[] pdf = historiaClinicaPdfService.generarPdfAltaUrgencia(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"alta-urgencia-" + id + ".pdf\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(pdf.length))
                .body(pdf);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        urgenciaRegistroService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
