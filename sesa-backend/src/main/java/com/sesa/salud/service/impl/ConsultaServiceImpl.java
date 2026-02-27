/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.ConsultaDto;
import com.sesa.salud.dto.ConsultaRequestDto;
import com.sesa.salud.entity.Cita;
import com.sesa.salud.entity.Consulta;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.entity.Personal;
import com.sesa.salud.repository.CitaRepository;
import com.sesa.salud.repository.ConsultaRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.repository.PersonalRepository;
import com.sesa.salud.security.JwtPrincipal;
import com.sesa.salud.service.ConsultaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsultaServiceImpl implements ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final PacienteRepository pacienteRepository;
    private final PersonalRepository personalRepository;
    private final CitaRepository citaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ConsultaDto> findAll(Pageable pageable) {
        return consultaRepository.findAll(pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultaDto> findByPacienteId(Long pacienteId, Pageable pageable) {
        return consultaRepository.findByPaciente_IdOrderByFechaConsultaDesc(pacienteId, pageable)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultaDto> findMisConsultas(Pageable pageable) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof JwtPrincipal principal)) {
            return Collections.emptyList();
        }
        var personal = personalRepository.findByUsuario_Id(principal.userId());
        if (personal.isEmpty()) {
            return Collections.emptyList();
        }
        return consultaRepository.findByProfesional_IdOrderByFechaConsultaDesc(personal.get().getId(), pageable)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultaDto findById(Long id) {
        Consulta c = consultaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Consulta no encontrada: " + id));
        return toDto(c);
    }

    @Override
    @Transactional
    public ConsultaDto create(ConsultaRequestDto dto) {
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + dto.getPacienteId()));
        Personal profesional = dto.getProfesionalId() != null
                ? personalRepository.findById(dto.getProfesionalId()).orElse(null)
                : null;
        Cita cita = dto.getCitaId() != null
                ? citaRepository.findById(dto.getCitaId()).orElse(null)
                : null;
        Consulta c = Consulta.builder()
                .paciente(paciente)
                .profesional(profesional)
                .cita(cita)
                .motivoConsulta(dto.getMotivoConsulta())
                .enfermedadActual(dto.getEnfermedadActual())
                .antecedentesPersonales(dto.getAntecedentesPersonales())
                .antecedentesFamiliares(dto.getAntecedentesFamiliares())
                .alergias(dto.getAlergias())
                .fechaConsulta(Instant.now())
                .tipoConsulta(dto.getTipoConsulta())
                .codigoCie10(dto.getCodigoCie10())
                .codigoCie10Secundario(dto.getCodigoCie10Secundario())
                .dolorEva(dto.getDolorEva())
                .perimetroAbdominal(dto.getPerimetroAbdominal())
                .perimetroCefalico(dto.getPerimetroCefalico())
                .saturacionO2(dto.getSaturacionO2())
                .presionArterial(dto.getPresionArterial())
                .frecuenciaCardiaca(dto.getFrecuenciaCardiaca())
                .frecuenciaRespiratoria(dto.getFrecuenciaRespiratoria())
                .temperatura(dto.getTemperatura())
                .peso(dto.getPeso())
                .talla(dto.getTalla())
                .imc(dto.getImc())
                .hallazgosExamen(dto.getHallazgosExamen())
                .diagnostico(dto.getDiagnostico())
                .planTratamiento(dto.getPlanTratamiento())
                .tratamientoFarmacologico(dto.getTratamientoFarmacologico())
                .observacionesClincias(dto.getObservacionesClincias())
                .recomendaciones(dto.getRecomendaciones())
                .build();
        c = consultaRepository.save(c);
        if (cita != null) {
            cita.setEstado("ATENDIDO");
            citaRepository.save(cita);
        }
        return toDto(c);
    }

    @Override
    @Transactional
    public ConsultaDto update(Long id, ConsultaRequestDto dto) {
        Consulta c = consultaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Consulta no encontrada: " + id));
        c.setMotivoConsulta(dto.getMotivoConsulta());
        c.setEnfermedadActual(dto.getEnfermedadActual());
        c.setAntecedentesPersonales(dto.getAntecedentesPersonales());
        c.setAntecedentesFamiliares(dto.getAntecedentesFamiliares());
        c.setAlergias(dto.getAlergias());
        if (dto.getProfesionalId() != null) {
            Personal profesional = personalRepository.findById(dto.getProfesionalId())
                    .orElseThrow(() -> new RuntimeException("Personal no encontrado: " + dto.getProfesionalId()));
            c.setProfesional(profesional);
        }
        if (dto.getCitaId() != null) {
            Cita cita = citaRepository.findById(dto.getCitaId())
                    .orElseThrow(() -> new RuntimeException("Cita no encontrada: " + dto.getCitaId()));
            c.setCita(cita);
        }
        c = consultaRepository.save(c);
        return toDto(c);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (!consultaRepository.existsById(id)) {
            throw new RuntimeException("Consulta no encontrada: " + id);
        }
        consultaRepository.deleteById(id);
    }

    private ConsultaDto toDto(Consulta c) {
        String pacienteNombre = c.getPaciente().getNombres() + " " + (c.getPaciente().getApellidos() != null ? c.getPaciente().getApellidos() : "");
        String profesionalNombre = c.getProfesional() != null
                ? c.getProfesional().getNombres() + " " + (c.getProfesional().getApellidos() != null ? c.getProfesional().getApellidos() : "")
                : null;
        return ConsultaDto.builder()
                .id(c.getId())
                .pacienteId(c.getPaciente().getId())
                .pacienteNombre(pacienteNombre.trim())
                .profesionalId(c.getProfesional() != null ? c.getProfesional().getId() : null)
                .profesionalNombre(profesionalNombre != null ? profesionalNombre.trim() : null)
                .profesionalTarjetaProfesional(c.getProfesional() != null ? c.getProfesional().getTarjetaProfesional() : null)
                .citaId(c.getCita() != null ? c.getCita().getId() : null)
                .motivoConsulta(c.getMotivoConsulta())
                .enfermedadActual(c.getEnfermedadActual())
                .antecedentesPersonales(c.getAntecedentesPersonales())
                .antecedentesFamiliares(c.getAntecedentesFamiliares())
                .alergias(c.getAlergias())
                .fechaConsulta(c.getFechaConsulta())
                .createdAt(c.getCreatedAt())
                .tipoConsulta(c.getTipoConsulta())
                .codigoCie10(c.getCodigoCie10())
                .codigoCie10Secundario(c.getCodigoCie10Secundario())
                .dolorEva(c.getDolorEva())
                .perimetroAbdominal(c.getPerimetroAbdominal())
                .perimetroCefalico(c.getPerimetroCefalico())
                .saturacionO2(c.getSaturacionO2())
                .presionArterial(c.getPresionArterial())
                .frecuenciaCardiaca(c.getFrecuenciaCardiaca())
                .frecuenciaRespiratoria(c.getFrecuenciaRespiratoria())
                .temperatura(c.getTemperatura())
                .peso(c.getPeso())
                .talla(c.getTalla())
                .imc(c.getImc())
                .hallazgosExamen(c.getHallazgosExamen())
                .diagnostico(c.getDiagnostico())
                .planTratamiento(c.getPlanTratamiento())
                .tratamientoFarmacologico(c.getTratamientoFarmacologico())
                .observacionesClincias(c.getObservacionesClincias())
                .recomendaciones(c.getRecomendaciones())
                .build();
    }
}
