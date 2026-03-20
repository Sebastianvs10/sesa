/**
 * Controller REST para el módulo Odontología.
 * Gestiona consultas, odontograma, planes de tratamiento, imágenes y evoluciones.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.odontologia.*;
import com.sesa.salud.entity.ProcedimientoCatalogo;
import com.sesa.salud.service.OdontologiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/odontologia")
@RequiredArgsConstructor
public class OdontologiaController {

    private final OdontologiaService odontologiaService;

    // ── Consultas odontológicas ──────────────────────────────────────────

    @GetMapping("/consultas/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO','ADMIN','COORDINADOR_MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<List<ConsultaOdontologicaDto>> consultasByPaciente(
            @PathVariable Long pacienteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(odontologiaService.getConsultasByPaciente(pacienteId, PageRequest.of(page, size)));
    }

    @GetMapping("/consultas/{id}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO','ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<ConsultaOdontologicaDto> getConsulta(@PathVariable Long id) {
        return ResponseEntity.ok(odontologiaService.getConsultaById(id));
    }

    @PostMapping("/consultas")
    @PreAuthorize("hasAnyRole('ODONTOLOGO','ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<ConsultaOdontologicaDto> crearConsulta(@RequestBody ConsultaOdontologicaDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(odontologiaService.crearConsulta(dto));
    }

    @PutMapping("/consultas/{id}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO','ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<ConsultaOdontologicaDto> actualizarConsulta(
            @PathVariable Long id, @RequestBody ConsultaOdontologicaDto dto) {
        return ResponseEntity.ok(odontologiaService.actualizarConsulta(id, dto));
    }

    // ── Odontograma ──────────────────────────────────────────────────────

    @GetMapping("/odontograma/{pacienteId}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO','ADMIN','COORDINADOR_MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<List<OdontogramaEstadoDto>> getOdontograma(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(odontologiaService.getOdontograma(pacienteId));
    }

    /** Cambios del odontograma registrados en una consulta específica. */
    @GetMapping("/odontograma/consulta/{consultaId}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO','ADMIN','COORDINADOR_MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<List<OdontogramaEstadoDto>> getOdontogramaByConsulta(@PathVariable Long consultaId) {
        return ResponseEntity.ok(odontologiaService.getOdontogramaByConsulta(consultaId));
    }

    @PostMapping("/odontograma")
    @PreAuthorize("hasAnyRole('ODONTOLOGO','ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<OdontogramaEstadoDto> guardarEstadoPieza(@RequestBody OdontogramaEstadoDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(odontologiaService.guardarEstadoPieza(dto));
    }

    @PostMapping("/odontograma/batch")
    @PreAuthorize("hasAnyRole('ODONTOLOGO','ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<List<OdontogramaEstadoDto>> guardarOdontogramaBatch(
            @RequestBody List<OdontogramaEstadoDto> cambios) {
        return ResponseEntity.ok(odontologiaService.guardarOdontogramaBatch(cambios));
    }

    // ── Catálogo de procedimientos ───────────────────────────────────────

    @GetMapping("/procedimientos/catalogo")
    @PreAuthorize("hasAnyRole('ODONTOLOGO','ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<List<ProcedimientoCatalogo>> getCatalogo() {
        return ResponseEntity.ok(odontologiaService.getCatalogoProcedimientos());
    }

    @PostMapping("/procedimientos/catalogo")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<ProcedimientoCatalogo> crearProcedimiento(@RequestBody ProcedimientoCatalogo dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(odontologiaService.crearProcedimientoCatalogo(dto));
    }

    @PutMapping("/procedimientos/catalogo/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<ProcedimientoCatalogo> actualizarProcedimiento(
            @PathVariable Long id, @RequestBody ProcedimientoCatalogo dto) {
        return ResponseEntity.ok(odontologiaService.actualizarProcedimientoCatalogo(id, dto));
    }

    // ── Planes de tratamiento ────────────────────────────────────────────

    @GetMapping("/planes/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO','ADMIN','COORDINADOR_MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<List<PlanTratamientoDto>> planesByPaciente(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(odontologiaService.getPlanesByPaciente(pacienteId));
    }

    @GetMapping("/planes/{id}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO','ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<PlanTratamientoDto> getPlan(@PathVariable Long id) {
        return ResponseEntity.ok(odontologiaService.getPlanById(id));
    }

    @PostMapping("/planes")
    @PreAuthorize("hasAnyRole('ODONTOLOGO','ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<PlanTratamientoDto> crearPlan(@RequestBody PlanTratamientoDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(odontologiaService.crearPlan(dto));
    }

    @PutMapping("/planes/{id}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO','ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<PlanTratamientoDto> actualizarPlan(
            @PathVariable Long id, @RequestBody PlanTratamientoDto dto) {
        return ResponseEntity.ok(odontologiaService.actualizarPlan(id, dto));
    }

    @PatchMapping("/planes/{id}/estado")
    @PreAuthorize("hasAnyRole('ODONTOLOGO','ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<PlanTratamientoDto> cambiarEstadoPlan(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(odontologiaService.cambiarEstadoPlan(id, body.get("estado")));
    }

    @PatchMapping("/planes/{id}/abono")
    @PreAuthorize("hasAnyRole('ODONTOLOGO','ADMIN','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<PlanTratamientoDto> registrarAbono(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        java.math.BigDecimal monto = new java.math.BigDecimal(body.get("monto").toString());
        return ResponseEntity.ok(odontologiaService.registrarAbono(id, monto));
    }

    // ── Imágenes clínicas ────────────────────────────────────────────────

    @GetMapping("/imagenes/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO','ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<List<ImagenClinicaDto>> imagenesByPaciente(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(odontologiaService.getImagenesByPaciente(pacienteId));
    }

    @PostMapping("/imagenes")
    @PreAuthorize("hasAnyRole('ODONTOLOGO','ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<ImagenClinicaDto> subirImagen(@RequestBody ImagenClinicaDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(odontologiaService.subirImagen(dto));
    }

    @DeleteMapping("/imagenes/{id}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO','ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<Void> eliminarImagen(@PathVariable Long id) {
        odontologiaService.eliminarImagen(id);
        return ResponseEntity.noContent().build();
    }

    // ── Evoluciones ──────────────────────────────────────────────────────

    @GetMapping("/evoluciones/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO','ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<List<EvolucionOdontologicaDto>> evolucionesByPaciente(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(odontologiaService.getEvolucionesByPaciente(pacienteId));
    }

    @PostMapping("/evoluciones")
    @PreAuthorize("hasAnyRole('ODONTOLOGO','ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<EvolucionOdontologicaDto> registrarEvolucion(@RequestBody EvolucionOdontologicaDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(odontologiaService.registrarEvolucion(dto));
    }

    // ── Dashboard stats ──────────────────────────────────────────────────

    @GetMapping("/stats/{profesionalId}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO','ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> statsDelDia(@PathVariable Long profesionalId) {
        return ResponseEntity.ok(odontologiaService.getStatsDelDia(profesionalId));
    }
}
