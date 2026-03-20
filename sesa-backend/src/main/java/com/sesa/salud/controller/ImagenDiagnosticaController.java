/**
 * Controlador de imágenes diagnósticas: listado por atención y listado global con filtros.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.ImagenDiagnosticaDto;
import com.sesa.salud.dto.ImagenDiagnosticaRequestDto;
import com.sesa.salud.service.ImagenDiagnosticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/imagenes-diagnosticas")
@RequiredArgsConstructor
public class ImagenDiagnosticaController {

    private final ImagenDiagnosticaService imagenDiagnosticaService;

    /**
     * Lista imágenes por ID de atención (comportamiento actual del front).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','COORDINADOR_MEDICO','ENFERMERIA','AUXILIAR')")
    public ResponseEntity<List<ImagenDiagnosticaDto>> listByAtencion(
            @RequestParam(required = false) Long atencionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        if (atencionId != null) {
            Pageable pageable = PageRequest.of(page, size);
            List<ImagenDiagnosticaDto> list = imagenDiagnosticaService.findByAtencionId(atencionId, pageable);
            return ResponseEntity.ok(list);
        }
        return ResponseEntity.ok(List.of());
    }

    /**
     * Listado global con filtros (paciente, atención, tipo, rango de fechas) y paginación.
     */
    @GetMapping("/global")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','COORDINADOR_MEDICO','ENFERMERIA','AUXILIAR')")
    public ResponseEntity<Page<ImagenDiagnosticaDto>> listGlobal(
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) Long atencionId,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ImagenDiagnosticaDto> result = imagenDiagnosticaService.findGlobal(
                pacienteId, atencionId, tipo, fechaDesde, fechaHasta, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','COORDINADOR_MEDICO','ENFERMERIA','AUXILIAR')")
    public ResponseEntity<ImagenDiagnosticaDto> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(imagenDiagnosticaService.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','COORDINADOR_MEDICO','ENFERMERIA','AUXILIAR')")
    public ResponseEntity<ImagenDiagnosticaDto> create(@RequestBody ImagenDiagnosticaRequestDto dto) {
        return ResponseEntity.ok(imagenDiagnosticaService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','COORDINADOR_MEDICO','ENFERMERIA','AUXILIAR')")
    public ResponseEntity<ImagenDiagnosticaDto> update(@PathVariable Long id, @RequestBody ImagenDiagnosticaRequestDto dto) {
        return ResponseEntity.ok(imagenDiagnosticaService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        imagenDiagnosticaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
