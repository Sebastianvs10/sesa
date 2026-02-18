/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.dto.AtencionDto;
import com.sesa.salud.dto.AtencionRequestDto;
import com.sesa.salud.service.AtencionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/atenciones")
@RequiredArgsConstructor
public class AtencionController {

    private final AtencionService atencionService;

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
}
