/**
 * Receta electrónica con token verificable (anti-falsificación).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.*;
import com.sesa.salud.entity.Atencion;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.entity.Personal;
import com.sesa.salud.entity.RecetaElectronica;
import com.sesa.salud.entity.RecetaMedicamento;
import com.sesa.salud.repository.AtencionRepository;
import com.sesa.salud.repository.RecetaElectronicaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecetaElectronicaService {

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"));

    private final AtencionRepository atencionRepository;
    private final RecetaElectronicaRepository recetaElectronicaRepository;

    @Transactional
    public RecetaElectronicaDto crearDesdeAtencion(Long atencionId, String observaciones) {
        Atencion atencion = atencionRepository.findById(atencionId)
                .orElseThrow(() -> new RuntimeException("Atención no encontrada: " + atencionId));
        Paciente paciente = atencion.getHistoriaClinica().getPaciente();
        Personal profesional = atencion.getProfesional();
        String pacienteNombre = (paciente.getNombres() != null ? paciente.getNombres() : "") + " " + (paciente.getApellidos() != null ? paciente.getApellidos() : "");
        String medicoNombre = (profesional.getNombres() != null ? profesional.getNombres() : "") + " " + (profesional.getApellidos() != null ? profesional.getApellidos() : "");

        RecetaElectronica receta = RecetaElectronica.builder()
                .atencionId(atencionId)
                .pacienteId(paciente.getId())
                .medicoNombre(medicoNombre.trim())
                .pacienteNombre(pacienteNombre.trim())
                .pacienteDocumento(paciente.getDocumento())
                .fechaEmision(Instant.now())
                .diagnostico(atencion.getDiagnostico())
                .observaciones(observaciones)
                .build();
        receta = recetaElectronicaRepository.save(receta);

        if (atencion.getFormulasMedicas() != null && !atencion.getFormulasMedicas().isEmpty()) {
            for (var fm : atencion.getFormulasMedicas()) {
                RecetaMedicamento rm = RecetaMedicamento.builder()
                        .receta(receta)
                        .medicamento(fm.getMedicamento())
                        .dosis(fm.getDosis())
                        .frecuencia(fm.getFrecuencia())
                        .duracion(fm.getDuracion())
                        .build();
                receta.getMedicamentos().add(rm);
            }
            receta = recetaElectronicaRepository.save(receta);
        }

        return toDto(receta);
    }

    @Transactional
    public RecetaElectronicaDto crearConFormulas(CrearRecetaConFormulasRequestDto request) {
        if (request.getMedicamentos() == null || request.getMedicamentos().isEmpty()) {
            throw new RuntimeException("Debe incluir al menos un medicamento");
        }
        RecetaElectronica receta = RecetaElectronica.builder()
                .pacienteId(request.getPacienteId())
                .consultaId(request.getConsultaId())
                .medicoNombre(request.getMedicoNombre() != null ? request.getMedicoNombre() : "Médico")
                .pacienteNombre(request.getPacienteNombre() != null ? request.getPacienteNombre() : "Paciente")
                .fechaEmision(Instant.now())
                .diagnostico(request.getDiagnostico())
                .observaciones(request.getObservaciones())
                .build();
        receta = recetaElectronicaRepository.save(receta);

        for (FormulaMedicaDto fm : request.getMedicamentos()) {
            if (fm.getMedicamento() == null || fm.getMedicamento().isBlank()) continue;
            RecetaMedicamento rm = RecetaMedicamento.builder()
                    .receta(receta)
                    .medicamento(fm.getMedicamento())
                    .dosis(fm.getDosis())
                    .frecuencia(fm.getFrecuencia())
                    .duracion(fm.getDuracion())
                    .build();
            receta.getMedicamentos().add(rm);
        }
        receta = recetaElectronicaRepository.save(receta);
        return toDto(receta);
    }

    @Transactional(readOnly = true)
    public RecetaVerificacionResponseDto verificar(String token) {
        Optional<RecetaElectronica> opt = recetaElectronicaRepository.findByTokenVerificacion(token);
        if (opt.isEmpty()) {
            return RecetaVerificacionResponseDto.builder()
                    .valida(false)
                    .mensaje("Receta no encontrada o enlace inválido.")
                    .build();
        }
        RecetaElectronica r = opt.get();
        if (r.getValidaHasta() != null && r.getValidaHasta().isBefore(Instant.now())) {
            return RecetaVerificacionResponseDto.builder()
                    .valida(false)
                    .mensaje("El período de validez de esta receta ha expirado.")
                    .build();
        }
        RecetaVerificacionResponseDto.RecetaVerificacionDataDto data = new RecetaVerificacionResponseDto.RecetaVerificacionDataDto(
                r.getPacienteNombre(),
                r.getMedicoNombre(),
                ISO_FORMAT.format(r.getFechaEmision()),
                r.getMedicamentos().stream()
                        .map(m -> FormulaMedicaDto.builder()
                                .medicamento(m.getMedicamento())
                                .dosis(m.getDosis())
                                .frecuencia(m.getFrecuencia())
                                .duracion(m.getDuracion())
                                .build())
                        .collect(Collectors.toList()),
                r.getDiagnostico()
        );
        return RecetaVerificacionResponseDto.builder()
                .valida(true)
                .receta(data)
                .build();
    }

    private RecetaElectronicaDto toDto(RecetaElectronica r) {
        List<FormulaMedicaDto> meds = r.getMedicamentos() != null
                ? r.getMedicamentos().stream()
                .map(m -> FormulaMedicaDto.builder()
                        .id(m.getId())
                        .medicamento(m.getMedicamento())
                        .dosis(m.getDosis())
                        .frecuencia(m.getFrecuencia())
                        .duracion(m.getDuracion())
                        .build())
                .collect(Collectors.toList())
                : new ArrayList<>();
        return RecetaElectronicaDto.builder()
                .id(r.getId())
                .tokenVerificacion(r.getTokenVerificacion())
                .urlVerificacion(null)
                .atencionId(r.getAtencionId())
                .consultaId(r.getConsultaId())
                .pacienteNombre(r.getPacienteNombre())
                .pacienteDocumento(r.getPacienteDocumento())
                .medicoNombre(r.getMedicoNombre())
                .medicoTarjetaProfesional(r.getMedicoTarjetaProfesional())
                .fechaEmision(ISO_FORMAT.format(r.getFechaEmision()))
                .medicamentos(meds)
                .diagnostico(r.getDiagnostico())
                .observaciones(r.getObservaciones())
                .validaHasta(r.getValidaHasta() != null ? ISO_FORMAT.format(r.getValidaHasta()) : null)
                .build();
    }
}
