/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.ProgramacionMesDto;
import com.sesa.salud.dto.ResumenProfesionalDto;
import com.sesa.salud.dto.TurnoDto;
import com.sesa.salud.dto.TurnoRequestDto;
import com.sesa.salud.entity.enums.ServicioClinico;
import com.sesa.salud.entity.enums.TipoTurno;
import com.sesa.salud.repository.UsuarioRepository;
import com.sesa.salud.security.JwtPrincipal;
import com.sesa.salud.service.TurnoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * API REST del módulo de Agenda de Turnos.
 *
 * <p>Roles habilitados:
 * <ul>
 *   <li>{@code COORDINADOR_MEDICO} — gestiona turnos médicos; puede aprobar y cerrar.</li>
 *   <li>{@code JEFE_ENFERMERIA}   — gestiona turnos de enfermería; puede enviar a revisión.</li>
 *   <li>{@code ADMIN} / {@code SUPERADMINISTRADOR} — acceso completo.</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/agenda")
@RequiredArgsConstructor
public class TurnoController {

    private final TurnoService    turnoService;
    private final UsuarioRepository usuarioRepository;

    // ── Constantes de roles ──────────────────────────────────────────────────

    private static final String ROL_AGENDA =
        "hasAnyRole('COORDINADOR_MEDICO','JEFE_ENFERMERIA','ADMIN','SUPERADMINISTRADOR')";

    private static final String ROL_APROBADOR =
        "hasAnyRole('COORDINADOR_MEDICO','ADMIN','SUPERADMINISTRADOR')";

    // ════════════════════════════════════════════════════════════════════════
    //  PROGRAMACIÓN MENSUAL
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Devuelve (o crea en BORRADOR) la programación de un mes.
     *
     * <p>GET /api/agenda/programacion?anio=2025&mes=2</p>
     */
    @GetMapping("/programacion")
    @PreAuthorize(ROL_AGENDA)
    public ResponseEntity<ProgramacionMesDto> getProgramacion(
            @RequestParam int anio,
            @RequestParam int mes,
            Authentication auth) {
        JwtPrincipal p = principal(auth);
        return ResponseEntity.ok(
            turnoService.getOrCrearProgramacion(anio, mes, p.userId(), nombreUsuario(p)));
    }

    /**
     * Jefe de Enfermería envía la programación a revisión del Coordinador.
     *
     * <p>POST /api/agenda/programacion/revision?anio=2025&mes=2</p>
     */
    @PostMapping("/programacion/revision")
    @PreAuthorize(ROL_AGENDA)
    public ResponseEntity<ProgramacionMesDto> enviarARevision(
            @RequestParam int anio,
            @RequestParam int mes,
            Authentication auth) {
        JwtPrincipal p = principal(auth);
        return ResponseEntity.ok(turnoService.enviarARevision(anio, mes, p.userId()));
    }

    /**
     * Coordinador Médico aprueba la programación del mes.
     *
     * <p>POST /api/agenda/programacion/aprobar?anio=2025&mes=2</p>
     */
    @PostMapping("/programacion/aprobar")
    @PreAuthorize(ROL_APROBADOR)
    public ResponseEntity<ProgramacionMesDto> aprobar(
            @RequestParam int anio,
            @RequestParam int mes,
            Authentication auth) {
        JwtPrincipal p = principal(auth);
        return ResponseEntity.ok(turnoService.aprobar(anio, mes, p.userId(), nombreUsuario(p)));
    }

    /**
     * Cierra el mes: bloquea ediciones futuras.
     *
     * <p>POST /api/agenda/programacion/cerrar?anio=2025&mes=2</p>
     */
    @PostMapping("/programacion/cerrar")
    @PreAuthorize(ROL_APROBADOR)
    public ResponseEntity<ProgramacionMesDto> cerrar(
            @RequestParam int anio,
            @RequestParam int mes,
            Authentication auth) {
        JwtPrincipal p = principal(auth);
        return ResponseEntity.ok(turnoService.cerrar(anio, mes, p.userId()));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TURNOS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Lista los turnos de un mes con filtros opcionales.
     *
     * <p>GET /api/agenda/turnos?anio=2025&mes=2[&servicio=URGENCIAS&tipoTurno=URG_DIA&personalId=5]</p>
     */
    @GetMapping("/turnos")
    @PreAuthorize(ROL_AGENDA)
    public List<TurnoDto> listar(
            @RequestParam int anio,
            @RequestParam int mes,
            @RequestParam(required = false) ServicioClinico servicio,
            @RequestParam(required = false) TipoTurno tipoTurno,
            @RequestParam(required = false) Long personalId) {
        return turnoService.listar(anio, mes, servicio, tipoTurno, personalId);
    }

    /**
     * Crea un turno con validación de reglas laborales colombianas.
     *
     * <p>POST /api/agenda/turnos</p>
     */
    @PostMapping("/turnos")
    @PreAuthorize(ROL_AGENDA)
    public ResponseEntity<TurnoDto> crear(
            @Valid @RequestBody TurnoRequestDto request,
            Authentication auth) {
        JwtPrincipal p = principal(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(turnoService.crear(request, p.userId()));
    }

    /**
     * Actualiza los datos de un turno existente.
     *
     * <p>PUT /api/agenda/turnos/{id}</p>
     */
    @PutMapping("/turnos/{id}")
    @PreAuthorize(ROL_AGENDA)
    public ResponseEntity<TurnoDto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody TurnoRequestDto request,
            Authentication auth) {
        JwtPrincipal p = principal(auth);
        return ResponseEntity.ok(turnoService.actualizar(id, request, p.userId()));
    }

    /**
     * Mueve un turno a otra fecha (drag &amp; drop).
     *
     * <p>PATCH /api/agenda/turnos/{id}/mover?fecha=2025-02-15</p>
     */
    @PatchMapping("/turnos/{id}/mover")
    @PreAuthorize(ROL_AGENDA)
    public ResponseEntity<TurnoDto> mover(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Authentication auth) {
        JwtPrincipal p = principal(auth);
        return ResponseEntity.ok(turnoService.moverFecha(id, fecha, p.userId()));
    }

    /**
     * Elimina un turno (solo en meses editables).
     *
     * <p>DELETE /api/agenda/turnos/{id}</p>
     */
    @DeleteMapping("/turnos/{id}")
    @PreAuthorize(ROL_AGENDA)
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        turnoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  RESÚMENES
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Resumen de horas de todos los profesionales en el mes.
     *
     * <p>GET /api/agenda/resumen?anio=2025&mes=2</p>
     */
    @GetMapping("/resumen")
    @PreAuthorize(ROL_AGENDA)
    public List<ResumenProfesionalDto> resumenMes(
            @RequestParam int anio,
            @RequestParam int mes) {
        return turnoService.resumenMes(anio, mes);
    }

    /**
     * Resumen de horas de un profesional específico.
     *
     * <p>GET /api/agenda/resumen/{personalId}?anio=2025&mes=2</p>
     */
    @GetMapping("/resumen/{personalId}")
    @PreAuthorize(ROL_AGENDA)
    public ResponseEntity<ResumenProfesionalDto> resumenProfesional(
            @PathVariable Long personalId,
            @RequestParam int anio,
            @RequestParam int mes) {
        return ResponseEntity.ok(turnoService.resumenProfesional(personalId, anio, mes));
    }

    /**
     * Catálogos de tipos de turno y servicios para el frontend.
     *
     * <p>GET /api/agenda/catalogos</p>
     */
    @GetMapping("/catalogos")
    @PreAuthorize(ROL_AGENDA)
    public ResponseEntity<Map<String, Object>> catalogos() {
        var tiposTurno = java.util.Arrays.stream(TipoTurno.values())
                .map(t -> Map.of(
                    "valor",            t.name(),
                    "etiqueta",         t.etiqueta,
                    "duracionHoras",    t.duracionHoras,
                    "horaInicio",       t.horaInicio,
                    "requiereDescanso", t.requiereDescansoExtendido))
                .toList();

        var servicios = java.util.Arrays.stream(ServicioClinico.values())
                .map(s -> Map.of("valor", s.name(), "etiqueta", s.etiqueta))
                .toList();

        return ResponseEntity.ok(Map.of(
            "tiposTurno", tiposTurno,
            "servicios",  servicios));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private JwtPrincipal principal(Authentication auth) {
        return (JwtPrincipal) auth.getPrincipal();
    }

    private String nombreUsuario(JwtPrincipal p) {
        return usuarioRepository.findById(p.userId())
                .map(u -> u.getNombreCompleto() != null ? u.getNombreCompleto() : p.username())
                .orElse(p.username());
    }
}
