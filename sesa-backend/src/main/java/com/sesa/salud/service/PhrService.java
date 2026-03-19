/**
 * S7: Historial portátil (PHR) — PDF o FHIR para el paciente.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.entity.Atencion;
import com.sesa.salud.entity.HistoriaClinica;
import com.sesa.salud.repository.AtencionRepository;
import com.sesa.salud.repository.HistoriaClinicaRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.service.fhir.RdaGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhrService {

    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final AtencionRepository atencionRepository;
    private final PacienteRepository pacienteRepository;
    private final HistoriaClinicaPdfService historiaClinicaPdfService;
    private final RdaGeneratorService rdaGeneratorService;

    /**
     * Genera el PHR en PDF (historia clínica completa del paciente).
     */
    @Transactional(readOnly = true)
    public byte[] generarPhrPdf(Long pacienteId) {
        validarPaciente(pacienteId);
        return historiaClinicaPdfService.generarPdfPorPaciente(pacienteId);
    }

    /**
     * Genera el PHR en FHIR R4 (Bundle tipo RDA-Paciente con la última atención).
     */
    @Transactional(readOnly = true)
    public String generarPhrFhir(Long pacienteId) {
        validarPaciente(pacienteId);
        Atencion ultima = obtenerUltimaAtencion(pacienteId);
        if (ultima == null) {
            throw new RuntimeException("No hay atenciones registradas para generar el historial portátil FHIR. Use el formato PDF.");
        }
        return rdaGeneratorService.generarRdaPaciente(ultima);
    }

    private void validarPaciente(Long pacienteId) {
        if (pacienteId == null) {
            throw new IllegalArgumentException("pacienteId es requerido");
        }
        pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + pacienteId));
        if (historiaClinicaRepository.findByPacienteId(pacienteId).isEmpty()) {
            throw new RuntimeException("El paciente no tiene historia clínica. No se puede generar PHR.");
        }
    }

    private Atencion obtenerUltimaAtencion(Long pacienteId) {
        HistoriaClinica hc = historiaClinicaRepository.findByPacienteId(pacienteId).orElse(null);
        if (hc == null) return null;
        List<Atencion> page = atencionRepository.findByHistoriaClinicaId(
                hc.getId(),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "fechaAtencion"))
        ).getContent();
        return page.isEmpty() ? null : page.get(0);
    }
}
