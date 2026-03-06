/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.CitaDto;
import com.sesa.salud.dto.CitaRequestDto;
import com.sesa.salud.dto.ConsultaMedicaDto;
import com.sesa.salud.dto.ConsultasStatsDto;
import com.sesa.salud.entity.Personal;
import com.sesa.salud.service.CitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;

    /** Todos los roles clínicos y administrativos pueden consultar citas por fecha. */
    private static final String ROLES_LECTURA_CITAS =
        "hasAnyRole('ADMIN','USER','MEDICO','ODONTOLOGO','ENFERMERO','JEFE_ENFERMERIA'," +
        "'AUXILIAR_ENFERMERIA','PSICOLOGO','BACTERIOLOGO','REGENTE_FARMACIA'," +
        "'RECEPCIONISTA','COORDINADOR_MEDICO','SUPERADMINISTRADOR')";

    @GetMapping
    @PreAuthorize(ROLES_LECTURA_CITAS)
    public List<CitaDto> listByFecha(
            @RequestParam(value = "fecha", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(value = "profesionalId", required = false) Long profesionalId) {
        LocalDate f = fecha != null ? fecha : LocalDate.now();
        if (profesionalId != null) {
            return citaService.findByFechaAndProfesionalId(f, profesionalId);
        }
        return citaService.findByFecha(f);
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize(ROLES_LECTURA_CITAS)
    public List<CitaDto> listByPaciente(@PathVariable("pacienteId") Long pacienteId,
                                        @RequestParam(value = "page", defaultValue = "0") int page,
                                        @RequestParam(value = "size", defaultValue = "20") int size) {
        return citaService.findByPacienteId(pacienteId, org.springframework.data.domain.PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(ROLES_LECTURA_CITAS)
    public ResponseEntity<CitaDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(citaService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','ODONTOLOGO','JEFE_ENFERMERIA'," +
                  "'RECEPCIONISTA','COORDINADOR_MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<CitaDto> create(@Valid @RequestBody CitaRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citaService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','ODONTOLOGO','JEFE_ENFERMERIA'," +
                  "'RECEPCIONISTA','COORDINADOR_MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<CitaDto> update(@PathVariable("id") Long id, @Valid @RequestBody CitaRequestDto dto) {
        return ResponseEntity.ok(citaService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        citaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Módulo Consulta Médica ─────────────────────────────────────────────

    /**
     * Lista enriquecida de citas para el módulo Consulta Médica.
     * Si el admin pasa profesionalId, filtra por ese profesional;
     * si es MEDICO/JEFE_ENFERMERIA el profesionalId debe venir del cliente.
     */
    @GetMapping("/consulta-medica")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','ODONTOLOGO','JEFE_ENFERMERIA','PSICOLOGO','COORDINADOR_MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<List<ConsultaMedicaDto>> consultaMedica(
            @RequestParam(value = "fecha", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(value = "profesionalId", required = false) Long profesionalId) {
        if (profesionalId != null) {
            return ResponseEntity.ok(citaService.findConsultasMedicas(profesionalId, fecha));
        }
        return ResponseEntity.ok(citaService.findConsultasMedicasTodas(fecha));
    }

    /** Estadísticas del día para el módulo Consulta Médica. */
    @GetMapping("/consulta-medica/stats")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','ODONTOLOGO','JEFE_ENFERMERIA','PSICOLOGO','COORDINADOR_MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<ConsultasStatsDto> statsDelDia(
            @RequestParam(value = "fecha", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(value = "profesionalId", required = false) Long profesionalId) {
        return ResponseEntity.ok(citaService.getStatsDelDia(profesionalId, fecha));
    }

    /** Lista de profesionales médicos para filtrado admin. */
    @GetMapping("/consulta-medica/profesionales")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','COORDINADOR_MEDICO')")
    public ResponseEntity<List<Map<String, Object>>> profesionalesMedicos() {
        List<Personal> lista = citaService.findProfesionalesMedicos();
        List<Map<String, Object>> result = lista.stream().map(p -> Map.<String, Object>of(
                "id", p.getId(),
                "nombre", (p.getNombres() + " " + (p.getApellidos() != null ? p.getApellidos() : "")).trim(),
                "rol", p.getRol() != null ? p.getRol() : ""
        )).toList();
        return ResponseEntity.ok(result);
    }

    /** Cancela una cita con motivo. */
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','ODONTOLOGO','JEFE_ENFERMERIA','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<CitaDto> cancelar(@PathVariable("id") Long id,
                                             @RequestBody Map<String, String> body) {
        String motivo = body.getOrDefault("motivo", "");
        return ResponseEntity.ok(citaService.cancelarCita(id, motivo));
    }

    /** Cambia el estado de una cita (ej. AGENDADA → ATENDIDA). */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','ODONTOLOGO','JEFE_ENFERMERIA','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<CitaDto> cambiarEstado(@PathVariable("id") Long id,
                                                  @RequestBody Map<String, String> body) {
        String estado = body.get("estado");
        if (estado == null || estado.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(citaService.cambiarEstado(id, estado));
    }
}
