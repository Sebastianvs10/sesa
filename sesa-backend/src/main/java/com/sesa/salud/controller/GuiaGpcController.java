/**
 * S15: Controlador de guías de práctica clínica (GPC) por CIE-10.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.dto.GuiaGpcRegistroVisualizacionDto;
import com.sesa.salud.dto.GuiaGpcSugerenciaDto;
import com.sesa.salud.service.GuiaGpcService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/guia-gpc")
@RequiredArgsConstructor
public class GuiaGpcController {

    private static final String ROLES_HC = "hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','ENFERMERO','JEFE_ENFERMERIA','COORDINADOR_MEDICO')";

    private final GuiaGpcService guiaGpcService;

    /** Sugerencias GPC por código CIE-10 (para formulario SOAP Análisis/Plan). */
    @GetMapping("/sugerir")
    @PreAuthorize(ROLES_HC)
    public ResponseEntity<List<GuiaGpcSugerenciaDto>> sugerir(
            @RequestParam("codigoCie10") String codigoCie10) {
        List<GuiaGpcSugerenciaDto> list = guiaGpcService.sugerirPorCie10(codigoCie10);
        return ResponseEntity.ok(list);
    }

    /** Registra que se mostró una guía al profesional (auditoría). */
    @PostMapping("/registrar-visualizacion")
    @PreAuthorize(ROLES_HC)
    public ResponseEntity<Void> registrarVisualizacion(@RequestBody GuiaGpcRegistroVisualizacionDto dto) {
        guiaGpcService.registrarVisualizacion(dto);
        return ResponseEntity.ok().build();
    }
}
