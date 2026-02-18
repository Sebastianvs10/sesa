/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.*;
import com.sesa.salud.entity.*;
import com.sesa.salud.repository.*;
import com.sesa.salud.service.AtencionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AtencionServiceImpl implements AtencionService {

    private final AtencionRepository atencionRepository;
    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final PersonalRepository personalRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AtencionDto> findByHistoriaId(Long historiaId, Pageable pageable) {
        return atencionRepository.findByHistoriaClinicaId(historiaId, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public AtencionDto findById(Long id) {
        Atencion a = atencionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Atención no encontrada: " + id));
        return toDto(a);
    }

    @Override
    @Transactional
    public AtencionDto create(AtencionRequestDto dto) {
        HistoriaClinica hc = historiaClinicaRepository.findById(dto.getHistoriaId())
                .orElseThrow(() -> new RuntimeException("Historia clínica no encontrada"));
        Personal prof = personalRepository.findById(dto.getProfesionalId())
                .orElseThrow(() -> new RuntimeException("Profesional no encontrado"));
        Atencion a = Atencion.builder()
                .historiaClinica(hc)
                .profesional(prof)
                .fechaAtencion(dto.getFechaAtencion() != null ? dto.getFechaAtencion() : Instant.now())
                .motivoConsulta(dto.getMotivoConsulta())
                .enfermedadActual(dto.getEnfermedadActual())
                .versionEnfermedad(dto.getVersionEnfermedad())
                .sintomasAsociados(dto.getSintomasAsociados())
                .factoresMejoran(dto.getFactoresMejoran())
                .factoresEmpeoran(dto.getFactoresEmpeoran())
                .revisionSistemas(dto.getRevisionSistemas())
                .presionArterial(dto.getPresionArterial())
                .frecuenciaCardiaca(dto.getFrecuenciaCardiaca())
                .frecuenciaRespiratoria(dto.getFrecuenciaRespiratoria())
                .temperatura(dto.getTemperatura())
                .peso(dto.getPeso())
                .talla(dto.getTalla())
                .imc(dto.getImc())
                .evaluacionGeneral(dto.getEvaluacionGeneral())
                .hallazgos(dto.getHallazgos())
                .diagnostico(dto.getDiagnostico())
                .codigoCie10(dto.getCodigoCie10())
                .planTratamiento(dto.getPlanTratamiento())
                .tratamientoFarmacologico(dto.getTratamientoFarmacologico())
                .ordenesMedicas(dto.getOrdenesMedicas())
                .examenesSolicitados(dto.getExamenesSolicitados())
                .incapacidad(dto.getIncapacidad())
                .recomendaciones(dto.getRecomendaciones())
                .build();
        a = atencionRepository.save(a);
        if (dto.getDiagnosticos() != null && !dto.getDiagnosticos().isEmpty()) {
            for (DiagnosticoDto dd : dto.getDiagnosticos()) {
                Diagnostico diag = Diagnostico.builder()
                        .atencion(a)
                        .codigoCie10(dd.getCodigoCie10())
                        .descripcion(dd.getDescripcion())
                        .tipo(dd.getTipo() != null ? dd.getTipo() : "PRINCIPAL")
                        .build();
                a.getDiagnosticos().add(diag);
            }
            a = atencionRepository.save(a);
        }
        if (dto.getProcedimientos() != null && !dto.getProcedimientos().isEmpty()) {
            for (ProcedimientoDto pd : dto.getProcedimientos()) {
                Procedimiento proc = Procedimiento.builder()
                        .atencion(a)
                        .codigoCups(pd.getCodigoCups())
                        .descripcion(pd.getDescripcion())
                        .build();
                a.getProcedimientos().add(proc);
            }
            a = atencionRepository.save(a);
        }
        if (dto.getFormulasMedicas() != null && !dto.getFormulasMedicas().isEmpty()) {
            for (FormulaMedicaDto fm : dto.getFormulasMedicas()) {
                FormulaMedica f = FormulaMedica.builder()
                        .atencion(a)
                        .medicamento(fm.getMedicamento())
                        .dosis(fm.getDosis())
                        .frecuencia(fm.getFrecuencia())
                        .duracion(fm.getDuracion())
                        .build();
                a.getFormulasMedicas().add(f);
            }
            a = atencionRepository.save(a);
        }
        return toDto(a);
    }

    @Override
    @Transactional
    public AtencionDto update(Long id, AtencionRequestDto dto) {
        Atencion a = atencionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Atención no encontrada: " + id));
        if (dto.getMotivoConsulta() != null) a.setMotivoConsulta(dto.getMotivoConsulta());
        if (dto.getEnfermedadActual() != null) a.setEnfermedadActual(dto.getEnfermedadActual());
        if (dto.getVersionEnfermedad() != null) a.setVersionEnfermedad(dto.getVersionEnfermedad());
        if (dto.getSintomasAsociados() != null) a.setSintomasAsociados(dto.getSintomasAsociados());
        if (dto.getFactoresMejoran() != null) a.setFactoresMejoran(dto.getFactoresMejoran());
        if (dto.getFactoresEmpeoran() != null) a.setFactoresEmpeoran(dto.getFactoresEmpeoran());
        if (dto.getRevisionSistemas() != null) a.setRevisionSistemas(dto.getRevisionSistemas());
        if (dto.getPresionArterial() != null) a.setPresionArterial(dto.getPresionArterial());
        if (dto.getFrecuenciaCardiaca() != null) a.setFrecuenciaCardiaca(dto.getFrecuenciaCardiaca());
        if (dto.getFrecuenciaRespiratoria() != null) a.setFrecuenciaRespiratoria(dto.getFrecuenciaRespiratoria());
        if (dto.getTemperatura() != null) a.setTemperatura(dto.getTemperatura());
        if (dto.getPeso() != null) a.setPeso(dto.getPeso());
        if (dto.getTalla() != null) a.setTalla(dto.getTalla());
        if (dto.getImc() != null) a.setImc(dto.getImc());
        if (dto.getEvaluacionGeneral() != null) a.setEvaluacionGeneral(dto.getEvaluacionGeneral());
        if (dto.getHallazgos() != null) a.setHallazgos(dto.getHallazgos());
        if (dto.getDiagnostico() != null) a.setDiagnostico(dto.getDiagnostico());
        if (dto.getCodigoCie10() != null) a.setCodigoCie10(dto.getCodigoCie10());
        if (dto.getPlanTratamiento() != null) a.setPlanTratamiento(dto.getPlanTratamiento());
        if (dto.getTratamientoFarmacologico() != null) a.setTratamientoFarmacologico(dto.getTratamientoFarmacologico());
        if (dto.getOrdenesMedicas() != null) a.setOrdenesMedicas(dto.getOrdenesMedicas());
        if (dto.getExamenesSolicitados() != null) a.setExamenesSolicitados(dto.getExamenesSolicitados());
        if (dto.getIncapacidad() != null) a.setIncapacidad(dto.getIncapacidad());
        if (dto.getRecomendaciones() != null) a.setRecomendaciones(dto.getRecomendaciones());
        a = atencionRepository.save(a);
        return toDto(a);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (!atencionRepository.existsById(id)) {
            throw new RuntimeException("Atención no encontrada: " + id);
        }
        atencionRepository.deleteById(id);
    }

    private AtencionDto toDto(Atencion a) {
        Personal p = a.getProfesional();
        String profNombre = (p.getNombres() != null ? p.getNombres() : "") + " " + (p.getApellidos() != null ? p.getApellidos() : "");
        return AtencionDto.builder()
                .id(a.getId())
                .historiaId(a.getHistoriaClinica().getId())
                .profesionalId(p.getId())
                .profesionalNombre(profNombre.trim())
                .fechaAtencion(a.getFechaAtencion())
                .motivoConsulta(a.getMotivoConsulta())
                .enfermedadActual(a.getEnfermedadActual())
                .versionEnfermedad(a.getVersionEnfermedad())
                .sintomasAsociados(a.getSintomasAsociados())
                .factoresMejoran(a.getFactoresMejoran())
                .factoresEmpeoran(a.getFactoresEmpeoran())
                .revisionSistemas(a.getRevisionSistemas())
                .presionArterial(a.getPresionArterial())
                .frecuenciaCardiaca(a.getFrecuenciaCardiaca())
                .frecuenciaRespiratoria(a.getFrecuenciaRespiratoria())
                .temperatura(a.getTemperatura())
                .peso(a.getPeso())
                .talla(a.getTalla())
                .imc(a.getImc())
                .evaluacionGeneral(a.getEvaluacionGeneral())
                .hallazgos(a.getHallazgos())
                .diagnostico(a.getDiagnostico())
                .codigoCie10(a.getCodigoCie10())
                .planTratamiento(a.getPlanTratamiento())
                .tratamientoFarmacologico(a.getTratamientoFarmacologico())
                .ordenesMedicas(a.getOrdenesMedicas())
                .examenesSolicitados(a.getExamenesSolicitados())
                .incapacidad(a.getIncapacidad())
                .recomendaciones(a.getRecomendaciones())
                .diagnosticos(a.getDiagnosticos() != null ? a.getDiagnosticos().stream()
                        .map(d -> DiagnosticoDto.builder().id(d.getId()).codigoCie10(d.getCodigoCie10())
                                .descripcion(d.getDescripcion()).tipo(d.getTipo()).build())
                        .collect(Collectors.toList()) : new ArrayList<>())
                .procedimientos(a.getProcedimientos() != null ? a.getProcedimientos().stream()
                        .map(pr -> ProcedimientoDto.builder().id(pr.getId()).codigoCups(pr.getCodigoCups())
                                .descripcion(pr.getDescripcion()).build())
                        .collect(Collectors.toList()) : new ArrayList<>())
                .formulasMedicas(a.getFormulasMedicas() != null ? a.getFormulasMedicas().stream()
                        .map(fm -> FormulaMedicaDto.builder().id(fm.getId()).medicamento(fm.getMedicamento())
                                .dosis(fm.getDosis()).frecuencia(fm.getFrecuencia()).duracion(fm.getDuracion()).build())
                        .collect(Collectors.toList()) : new ArrayList<>())
                .build();
    }
}
