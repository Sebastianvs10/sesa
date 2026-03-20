/**
 * Controlador global de exportación e importación de datos (Excel)
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.dto.ExcelImportResultDto;
import com.sesa.salud.dto.FacturaDto;
import com.sesa.salud.dto.PacienteDto;
import com.sesa.salud.service.ExcelExportService;
import com.sesa.salud.service.ExcelImportService;
import com.sesa.salud.service.FacturaService;
import com.sesa.salud.service.PacienteService;
import com.sesa.salud.tenant.TenantContextHolder;
import com.sesa.salud.service.EmpresaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/data-exchange")
@RequiredArgsConstructor
public class DataExchangeController {

    private final ExcelExportService exportService;
    private final ExcelImportService importService;
    private final PacienteService pacienteService;
    private final FacturaService facturaService;
    private final EmpresaService empresaService;

    /* ════════════════════════ EXPORT ═════════════════════════════════ */

    /** Exporta todos los pacientes a Excel. */
    @GetMapping("/export/pacientes")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','FACTURACION')")
    public ResponseEntity<byte[]> exportPacientes() {
        String empresa = getEmpresaNombre();
        List<PacienteDto> pacientes = pacienteService.findAll(PageRequest.of(0, 10000)).getContent();
        byte[] excel = exportService.exportarPacientes(pacientes, empresa);
        return excelResponse(excel, "pacientes.xlsx");
    }

    /** Exporta facturas a Excel con filtro de fechas opcional. */
    @GetMapping("/export/facturas")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','FACTURACION')")
    public ResponseEntity<byte[]> exportFacturas(
            @RequestParam(value = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        String empresa = getEmpresaNombre();
        Instant desdeInst = desde != null ? desde.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant hastaInst = hasta != null ? hasta.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : null;

        List<FacturaDto> facturas = facturaService
                .findAllFiltered(null, desdeInst, hastaInst, null, PageRequest.of(0, 50000))
                .getContent();

        byte[] excel = exportService.exportarFacturas(facturas, empresa);
        String filename = "facturas" + (desde != null ? "_" + desde + "_" + hasta : "") + ".xlsx";
        return excelResponse(excel, filename);
    }

    /* ════════════════════════ IMPORT ═════════════════════════════════ */

    /** Importa pacientes desde Excel. */
    @PostMapping("/import/pacientes")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<ExcelImportResultDto> importPacientes(
            @RequestParam("file") MultipartFile file) {
        ExcelImportResultDto result = importService.importarPacientes(file);
        return ResponseEntity.ok(result);
    }

    /** Importa facturas desde Excel. */
    @PostMapping("/import/facturas")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','FACTURACION')")
    public ResponseEntity<ExcelImportResultDto> importFacturas(
            @RequestParam("file") MultipartFile file) {
        ExcelImportResultDto result = importService.importarFacturas(file);
        return ResponseEntity.ok(result);
    }

    /* ════════════════════════ Plantilla descargable ══════════════════ */

    /** Descarga plantilla Excel vacía para pacientes. */
    @GetMapping("/template/pacientes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> templatePacientes() {
        byte[] template = exportService.exportarPacientes(List.of(), getEmpresaNombre());
        return excelResponse(template, "plantilla_pacientes.xlsx");
    }

    /** Descarga plantilla Excel vacía para facturas. */
    @GetMapping("/template/facturas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> templateFacturas() {
        byte[] template = exportService.exportarFacturas(List.of(), getEmpresaNombre());
        return excelResponse(template, "plantilla_facturas.xlsx");
    }

    /* ════════════════════════ Helpers ═══════════════════════════════ */

    private ResponseEntity<byte[]> excelResponse(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length))
                .body(bytes);
    }

    private String getEmpresaNombre() {
        try {
            return empresaService.findBySchemaName(TenantContextHolder.getTenantSchema())
                    .map(e -> e.getRazonSocial())
                    .orElse("SESA Salud");
        } catch (Exception e) {
            return "SESA Salud";
        }
    }
}
