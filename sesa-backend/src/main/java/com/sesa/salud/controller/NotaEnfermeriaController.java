/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.NotaEnfermeriaDto;
import com.sesa.salud.dto.NotaEnfermeriaRequestDto;
import com.sesa.salud.service.NotaEnfermeriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notas-enfermeria")
@RequiredArgsConstructor
public class NotaEnfermeriaController {

    private final NotaEnfermeriaService notaEnfermeriaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','COORDINADOR_MEDICO','JEFE_ENFERMERIA','ENFERMERO','AUXILIAR_ENFERMERIA')")
    public ResponseEntity<List<NotaEnfermeriaDto>> listByAtencion(
            @RequestParam("atencionId") Long atencionId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(notaEnfermeriaService.findByAtencionId(atencionId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','COORDINADOR_MEDICO','JEFE_ENFERMERIA','ENFERMERO','AUXILIAR_ENFERMERIA')")
    public ResponseEntity<NotaEnfermeriaDto> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(notaEnfermeriaService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','JEFE_ENFERMERIA','ENFERMERO','AUXILIAR_ENFERMERIA')")
    public ResponseEntity<NotaEnfermeriaDto> create(@Valid @RequestBody NotaEnfermeriaRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notaEnfermeriaService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','JEFE_ENFERMERIA','ENFERMERO')")
    public ResponseEntity<NotaEnfermeriaDto> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody NotaEnfermeriaRequestDto dto) {
        return ResponseEntity.ok(notaEnfermeriaService.update(id, dto));
    }
}
