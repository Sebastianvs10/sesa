/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.dto.AlertasFacturacionDto;
import com.sesa.salud.dto.BandejaFacturadorDto;
import com.sesa.salud.dto.ChecklistRadicacionDto;
import com.sesa.salud.dto.FacturaDto;
import com.sesa.salud.dto.FacturaRequestDto;
import com.sesa.salud.dto.ResumenFacturacionDto;
import com.sesa.salud.dto.RipsGenerarRequestDto;
import com.sesa.salud.dto.FacturaDetalleCompletoDto;
import com.sesa.salud.dto.FacturaLoteRequestDto;
import com.sesa.salud.dto.FacturaLoteResultDto;
import com.sesa.salud.dto.FacturaTimelineEventDto;
import com.sesa.salud.dto.OrdenPendienteFacturaDto;
import com.sesa.salud.service.FacturaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/facturas")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturaService facturaService;

    /** Listado general con filtros opcionales (estado, rango de fechas, paciente). */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public Page<FacturaDto> listAll(
            @RequestParam(value = "estado", required = false) String estado,
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(value = "pacienteId", required = false) Long pacienteId,
            @PageableDefault(size = 20, sort = "fechaFactura") Pageable pageable) {

        Instant desdeInst = desde != null ? desde.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant hastaInst = hasta != null ? hasta.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        return facturaService.findAllFiltered(estado, desdeInst, hastaInst, pacienteId, pageable);
    }

    /** Facturas de un paciente específico. */
    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public List<FacturaDto> listByPaciente(@PathVariable("pacienteId") Long pacienteId,
                                           @PageableDefault(size = 20) Pageable pageable) {
        return facturaService.findByPacienteId(pacienteId, pageable);
    }

    /** KPI resumen del módulo de facturación. */
    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<ResumenFacturacionDto> resumen() {
        return ResponseEntity.ok(facturaService.resumen());
    }

    /** Órdenes clínicas sin factura asociada (medicamentos, laboratorio, procedimientos) — pendientes de facturar. */
    @GetMapping("/ordenes-pendientes")
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public Page<OrdenPendienteFacturaDto> ordenesPendientesDeFacturar(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return facturaService.getOrdenesPendientesDeFacturar(pageable);
    }

    /** Alertas para dashboard predictivo (facturas por vencer/vencidas radicación, glosas pendientes). */
    @GetMapping("/alertas")
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<AlertasFacturacionDto> alertas() {
        return ResponseEntity.ok(facturaService.getAlertas());
    }

    /** Checklist pre-radicación: validaciones antes de radicar la factura. */
    @GetMapping("/{id:\\d+}/checklist-radicacion")
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<ChecklistRadicacionDto> checklistRadicacion(@PathVariable("id") Long id) {
        return ResponseEntity.ok(facturaService.getChecklistRadicacion(id));
    }

    /** Bandeja del facturador: tareas (radicar, responder glosas). */
    @GetMapping("/bandeja-facturador")
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<BandejaFacturadorDto> bandejaFacturador() {
        return ResponseEntity.ok(facturaService.getBandejaFacturador());
    }

    /** Libro de facturación: exportar CSV por período (y estado opcional). */
    @GetMapping(value = "/reporte-libro/export", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<String> exportLibro(
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(value = "estado", required = false) String estado) {
        Instant desdeInst = desde.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant hastaInst = hasta.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        String csv = facturaService.exportLibroCsv(desdeInst, hastaInst, estado);
        String filename = "libro_facturacion_" + desde + "_" + hasta + ".csv";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(csv);
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<FacturaDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(facturaService.findById(id));
    }

    /** Detalle con trazabilidad a orden médica y consulta (historia clínica). */
    @GetMapping("/{id:\\d+}/detalle-completo")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<FacturaDetalleCompletoDto> getDetalleCompleto(@PathVariable("id") Long id) {
        return ResponseEntity.ok(facturaService.findDetalleCompleto(id));
    }

    /** Línea de tiempo de la factura (creada → FEV → radicada → glosa → estado). */
    @GetMapping("/{id:\\d+}/timeline")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<List<FacturaTimelineEventDto>> getTimeline(@PathVariable("id") Long id) {
        return ResponseEntity.ok(facturaService.getTimeline(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<FacturaDto> create(@Valid @RequestBody FacturaRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facturaService.create(dto));
    }

    /** Facturación por lote: agrupa órdenes seleccionadas por paciente y crea una factura por paciente. */
    @PostMapping("/lote")
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<FacturaLoteResultDto> createFromLote(@Valid @RequestBody FacturaLoteRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facturaService.createFromLote(request.getOrdenIds()));
    }

    /** Emite una factura como electrónica ante la DIAN (si la empresa tiene configuración activa). */
    @PostMapping("/{id:\\d+}/emitir-electronica")
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<FacturaDto> emitirElectronica(@PathVariable("id") Long id) {
        return ResponseEntity.ok(facturaService.emitirElectronica(id));
    }

    @PutMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<FacturaDto> update(@PathVariable("id") Long id, @Valid @RequestBody FacturaRequestDto dto) {
        return ResponseEntity.ok(facturaService.update(id, dto));
    }

    /** Cambiar estado de una factura (PENDIENTE → PAGADA, EN_PROCESO, RECHAZADA, ANULADA). */
    @PatchMapping("/{id:\\d+}/estado")
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<FacturaDto> cambiarEstado(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        String estado = body.get("estado");
        if (estado == null || estado.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(facturaService.cambiarEstado(id, estado));
    }

    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        facturaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** Exportación RIPS CSV genérico (compatible con versiones anteriores). */
    @GetMapping(value = "/rips", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<String> exportRips(
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        Instant d = desde.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant h = hasta.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        String csv = facturaService.exportRipsCsv(d, h);
        String filename = "RIPS_" + desde + "_" + hasta + ".csv";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(csv);
    }

    /** Exportación RIPS estructurado Res. 3374/2000 (archivos CT, US, AP, AC). */
    @GetMapping(value = "/rips/estructurado", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<Map<String, String>> exportRipsEstructurado(
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        Instant d = desde.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant h = hasta.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Map<String, String> archivos = facturaService.exportRipsEstructurado(d, h);
        return ResponseEntity.ok(archivos);
    }

    /** Generación automática de RIPS para un periodo. Si no se envía cuerpo, se usa el mes anterior. */
    @PostMapping(value = "/rips/generar-automatico", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<Map<String, String>> generarRipsAutomatico(
            @RequestBody(required = false) RipsGenerarRequestDto request) {
        YearMonth mesAnterior = YearMonth.now().minusMonths(1);
        LocalDate desde = request != null && request.getDesde() != null && !request.getDesde().isBlank()
                ? LocalDate.parse(request.getDesde())
                : mesAnterior.atDay(1);
        LocalDate hasta = request != null && request.getHasta() != null && !request.getHasta().isBlank()
                ? LocalDate.parse(request.getHasta())
                : mesAnterior.atEndOfMonth();
        Instant d = desde.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant h = hasta.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Map<String, String> archivos = facturaService.exportRipsEstructurado(d, h);
        return ResponseEntity.ok(archivos);
    }
}
