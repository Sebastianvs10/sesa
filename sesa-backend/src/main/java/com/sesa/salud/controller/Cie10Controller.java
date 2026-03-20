/**
 * S8: Endpoint de sugerencias CIE-10.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.dto.Cie10SugerenciaDto;
import com.sesa.salud.service.Cie10SugerenciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cie10")
@RequiredArgsConstructor
public class Cie10Controller {

    private final Cie10SugerenciaService cie10SugerenciaService;

    /**
     * GET /cie10/sugerir?motivo=&texto=
     * Sugiere códigos CIE-10 a partir del motivo de consulta y texto de análisis/diagnóstico.
     */
    @GetMapping("/sugerir")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Cie10SugerenciaDto>> sugerir(
            @RequestParam(value = "motivo", required = false) String motivo,
            @RequestParam(value = "texto", required = false) String texto) {
        List<Cie10SugerenciaDto> lista = cie10SugerenciaService.sugerir(motivo, texto);
        return ResponseEntity.ok(lista);
    }
}
