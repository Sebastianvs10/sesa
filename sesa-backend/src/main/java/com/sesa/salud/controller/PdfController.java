/**
 * Controlador de generación de documentos PDF
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.service.HistoriaClinicaPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pdf")
@RequiredArgsConstructor
public class PdfController {

    private final HistoriaClinicaPdfService pdfService;

    /** PDF de historia clínica completa por ID de historia. */
    @GetMapping("/historia/{historiaId:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','USER','FACTURACION','SUPERADMINISTRADOR')")
    public ResponseEntity<byte[]> historiaClinicaPorId(@PathVariable("historiaId") Long historiaId) {
        byte[] pdf = pdfService.generarPdf(historiaId);
        return buildPdfResponse(pdf, "historia-clinica-" + historiaId + ".pdf");
    }

    /** PDF de historia clínica completa buscando por ID de paciente. */
    @GetMapping("/historia/paciente/{pacienteId:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','USER','FACTURACION','SUPERADMINISTRADOR')")
    public ResponseEntity<byte[]> historiaClinicaPorPaciente(@PathVariable("pacienteId") Long pacienteId) {
        byte[] pdf = pdfService.generarPdfPorPaciente(pacienteId);
        return buildPdfResponse(pdf, "historia-clinica-paciente-" + pacienteId + ".pdf");
    }

    /** PDF de órdenes clínicas y resultados del paciente. */
    @GetMapping("/ordenes/paciente/{pacienteId:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','USER','FACTURACION','SUPERADMINISTRADOR','BACTERIOLOGO','ENFERMERO','JEFE_ENFERMERIA')")
    public ResponseEntity<byte[]> ordenesPorPaciente(@PathVariable("pacienteId") Long pacienteId) {
        byte[] pdf = pdfService.generarPdfOrdenesPaciente(pacienteId);
        return buildPdfResponse(pdf, "ordenes-resultados-paciente-" + pacienteId + ".pdf");
    }

    /** PDF de una sola orden clínica (con datos del paciente y resultado si existe). */
    @GetMapping("/orden/{ordenId:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','USER','FACTURACION','SUPERADMINISTRADOR','BACTERIOLOGO','ENFERMERO','JEFE_ENFERMERIA')")
    public ResponseEntity<byte[]> ordenIndividual(@PathVariable("ordenId") Long ordenId) {
        byte[] pdf = pdfService.generarPdfOrdenIndividual(ordenId);
        return buildPdfResponse(pdf, "orden-" + ordenId + ".pdf");
    }

    private ResponseEntity<byte[]> buildPdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(pdf.length))
                .body(pdf);
    }
}
