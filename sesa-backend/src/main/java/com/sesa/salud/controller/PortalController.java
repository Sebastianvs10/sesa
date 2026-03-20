/**
 * Endpoints del Portal del Paciente (y acceso móvil).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.ConsentimientoInformadoDto;
import com.sesa.salud.dto.CuestionarioPreconsultaDto;
import com.sesa.salud.dto.CuestionarioPreconsultaRequestDto;
import com.sesa.salud.dto.OrdenConResultadoPortalDto;
import com.sesa.salud.entity.ConsentimientoInformado;
import com.sesa.salud.entity.OrdenClinica;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.repository.ConsentimientoInformadoRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.repository.OrdenClinicaRepository;
import com.sesa.salud.security.JwtPrincipal;
import com.sesa.salud.service.PhrService;
import com.sesa.salud.service.InterpretacionResultadoService;
import com.sesa.salud.service.HistoriaClinicaPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/portal")
@RequiredArgsConstructor
public class PortalController {

    private final PacienteRepository pacienteRepository;
    private final ConsentimientoInformadoRepository consentimientoRepository;
    private final OrdenClinicaRepository ordenClinicaRepository;
    private final InterpretacionResultadoService interpretacionResultadoService;
    private final HistoriaClinicaPdfService pdfService;
    private final PhrService phrService;
    private final com.sesa.salud.service.CuestionarioPreconsultaService cuestionarioPreconsultaService;

    /**
     * Consentimientos pendientes de firma del paciente actual (portal/móvil).
     * El usuario debe estar vinculado a un paciente (paciente.usuarioId).
     */
    @GetMapping("/consentimientos-pendientes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ConsentimientoInformadoDto>> consentimientosPendientes(Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        Paciente paciente = pacienteRepository.findByUsuarioId(principal.userId())
                .orElse(null);
        if (paciente == null) {
            return ResponseEntity.ok(List.of());
        }
        List<ConsentimientoInformado> list = consentimientoRepository
                .findByPaciente_IdAndEstadoOrderByCreatedAtDesc(paciente.getId(), "PENDIENTE");
        List<ConsentimientoInformadoDto> dtos = list.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /** S7: PHR del paciente autenticado (portal). Sin pacienteId en path. */
    @GetMapping("/phr")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> phrMi(
            @RequestParam(value = "formato", defaultValue = "pdf") String formato,
            Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        Paciente p = pacienteRepository.findByUsuarioId(principal.userId())
                .orElse(null);
        if (p == null) {
            return ResponseEntity.status(403).body("Usuario no vinculado a un paciente.");
        }
        return phr(p.getId(), formato, authentication);
    }

    /**
     * S7: Historial portátil (PHR). PDF o FHIR.
     * Paciente: solo su propio pacienteId (usuario vinculado). Profesionales: cualquier paciente con permiso HC.
     */
    @GetMapping("/paciente/{pacienteId}/phr")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> phr(
            @PathVariable("pacienteId") Long pacienteId,
            @RequestParam(value = "formato", defaultValue = "pdf") String formato,
            Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        Paciente pacienteActual = pacienteRepository.findByUsuarioId(principal.userId()).orElse(null);
        boolean esPropioPaciente = pacienteActual != null && pacienteActual.getId().equals(pacienteId);
        boolean puedeVerHc = principal.roles() != null && (
                principal.roles().contains("ADMIN") || principal.roles().contains("SUPERADMINISTRADOR")
                || principal.roles().contains("MEDICO") || principal.roles().contains("ODONTOLOGO")
                || principal.roles().contains("ENFERMERO") || principal.roles().contains("JEFE_ENFERMERIA")
                || principal.roles().contains("COORDINADOR_MEDICO") || principal.roles().contains("FACTURACION"));
        if (!esPropioPaciente && !puedeVerHc) {
            return ResponseEntity.status(403).build();
        }
        String fmt = formato != null ? formato.trim().toLowerCase() : "pdf";
        if ("fhir".equals(fmt)) {
            String json = phrService.generarPhrFhir(pacienteId);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"phr-" + pacienteId + ".json\"")
                    .body(json);
        }
        byte[] pdf = phrService.generarPhrPdf(pacienteId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"phr-" + pacienteId + ".pdf\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(pdf.length))
                .body(pdf);
    }

    /** S10: Enviar cuestionario pre-consulta (ePRO) para una cita. El paciente debe ser el de la cita (sesión portal). */
    @PostMapping("/cita/{citaId}/cuestionario-preconsulta")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> crearCuestionarioPreconsulta(
            @PathVariable("citaId") Long citaId,
            @RequestBody CuestionarioPreconsultaRequestDto body,
            Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        Paciente paciente = pacienteRepository.findByUsuarioId(principal.userId()).orElse(null);
        if (paciente == null) {
            return ResponseEntity.status(403).body("Usuario no vinculado a un paciente.");
        }
        CuestionarioPreconsultaRequestDto dto = CuestionarioPreconsultaRequestDto.builder()
                .citaId(citaId)
                .motivoPalabras(body.getMotivoPalabras())
                .dolorEva(body.getDolorEva())
                .ansiedadEva(body.getAnsiedadEva())
                .medicamentosActuales(body.getMedicamentosActuales())
                .alergiasReferidas(body.getAlergiasReferidas())
                .build();
        CuestionarioPreconsultaDto created = cuestionarioPreconsultaService.create(dto, paciente.getId());
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(created);
    }

    /** S14: Órdenes con resultado del paciente actual (portal), con interpretación en lenguaje sencillo y enlace a PDF. */
    @GetMapping("/paciente/ordenes-con-resultados")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrdenConResultadoPortalDto>> ordenesConResultados(Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        Paciente paciente = pacienteRepository.findByUsuarioId(principal.userId()).orElse(null);
        if (paciente == null) {
            return ResponseEntity.ok(List.of());
        }
        List<OrdenClinica> ordenes = ordenClinicaRepository.findByPaciente_IdAndResultadoNotNullOrderByFechaResultadoDesc(paciente.getId());
        List<OrdenConResultadoPortalDto> dtos = new ArrayList<>();
        for (OrdenClinica o : ordenes) {
            String interpretacion = interpretacionResultadoService.getInterpretacionLenguajeSencillo(o.getTipo(), o.getResultado());
            dtos.add(OrdenConResultadoPortalDto.builder()
                    .ordenId(o.getId())
                    .tipo(o.getTipo())
                    .detalle(o.getDetalle())
                    .resultado(o.getResultado())
                    .fechaResultado(o.getFechaResultado())
                    .interpretacionLenguajeSencillo(interpretacion)
                    .enlaceDescargaPdf("/api/portal/paciente/orden/" + o.getId() + "/pdf")
                    .build());
        }
        return ResponseEntity.ok(dtos);
    }

    /** S14: Descarga PDF de una orden del paciente actual (solo si la orden es del paciente). */
    @GetMapping("/paciente/orden/{ordenId}/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> descargarPdfOrdenPaciente(
            @PathVariable("ordenId") Long ordenId,
            Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        Paciente paciente = pacienteRepository.findByUsuarioId(principal.userId()).orElse(null);
        if (paciente == null) {
            return ResponseEntity.status(403).build();
        }
        OrdenClinica orden = ordenClinicaRepository.findById(ordenId).orElse(null);
        if (orden == null || !orden.getPaciente().getId().equals(paciente.getId())) {
            return ResponseEntity.status(404).build();
        }
        byte[] pdf = pdfService.generarPdfOrdenIndividual(ordenId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"orden-" + ordenId + ".pdf\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(pdf.length))
                .body(pdf);
    }

    private ConsentimientoInformadoDto toDto(ConsentimientoInformado c) {
        var p = c.getPaciente();
        var prof = c.getProfesional();
        return ConsentimientoInformadoDto.builder()
                .id(c.getId())
                .pacienteId(p.getId())
                .pacienteNombre((p.getNombres() + " " + (p.getApellidos() != null ? p.getApellidos() : "")).trim())
                .pacienteDocumento(p.getDocumento())
                .profesionalId(prof.getId())
                .profesionalNombre((prof.getNombres() + " " + (prof.getApellidos() != null ? prof.getApellidos() : "")).trim())
                .tipo(c.getTipo())
                .estado(c.getEstado())
                .procedimiento(c.getProcedimiento())
                .fechaSolicitud(c.getFechaSolicitud())
                .fechaFirma(c.getFechaFirma())
                .observaciones(c.getObservaciones())
                .firmaCanvasData(c.getFirmaCanvasData())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
