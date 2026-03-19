/**
 * API del catálogo CUPS (Colombia) para facturación y órdenes.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.dto.CupCatalogoDto;
import com.sesa.salud.service.CupCatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/cups-catalogo")
@RequiredArgsConstructor
public class CupCatalogoController {

    private final CupCatalogoService cupCatalogoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','FACTURACION','ENFERMERO','JEFE_ENFERMERIA','BACTERIOLOGO','COORDINADOR_MEDICO')")
    public ResponseEntity<List<CupCatalogoDto>> listar() {
        return ResponseEntity.ok(cupCatalogoService.listarActivos());
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','FACTURACION','ENFERMERO','JEFE_ENFERMERIA','BACTERIOLOGO','COORDINADOR_MEDICO')")
    public ResponseEntity<List<CupCatalogoDto>> buscar(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "100") int limit) {
        return ResponseEntity.ok(cupCatalogoService.buscar(q, limit));
    }

    @GetMapping("/codigo/{codigo}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','FACTURACION','ENFERMERO','JEFE_ENFERMERIA','BACTERIOLOGO','COORDINADOR_MEDICO')")
    public ResponseEntity<CupCatalogoDto> porCodigo(@PathVariable String codigo) {
        Optional<CupCatalogoDto> dto = cupCatalogoService.porCodigo(codigo);
        return dto.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
