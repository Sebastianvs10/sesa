/**
 * S10: Cuestionarios pre-consulta (ePRO).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.CuestionarioPreconsultaDto;
import com.sesa.salud.dto.CuestionarioPreconsultaRequestDto;
import com.sesa.salud.entity.Cita;
import com.sesa.salud.entity.Consulta;
import com.sesa.salud.entity.CuestionarioPreconsulta;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.repository.CitaRepository;
import com.sesa.salud.repository.CuestionarioPreconsultaRepository;
import com.sesa.salud.repository.ConsultaRepository;
import com.sesa.salud.service.CuestionarioPreconsultaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CuestionarioPreconsultaServiceImpl implements CuestionarioPreconsultaService {

    private final CuestionarioPreconsultaRepository cuestionarioPreconsultaRepository;
    private final CitaRepository citaRepository;
    private final ConsultaRepository consultaRepository;

    @Override
    @Transactional
    public CuestionarioPreconsultaDto create(CuestionarioPreconsultaRequestDto dto, Long pacienteIdAutenticado) {
        Cita cita = citaRepository.findById(dto.getCitaId())
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada: " + dto.getCitaId()));
        if (pacienteIdAutenticado != null && !pacienteIdAutenticado.equals(cita.getPaciente().getId())) {
            throw new IllegalArgumentException("La cita no corresponde al paciente autenticado.");
        }
        if (cuestionarioPreconsultaRepository.findByCitaId(cita.getId()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un cuestionario enviado para esta cita.");
        }
        CuestionarioPreconsulta q = CuestionarioPreconsulta.builder()
                .cita(cita)
                .paciente(cita.getPaciente())
                .motivoPalabras(dto.getMotivoPalabras())
                .dolorEva(dto.getDolorEva())
                .ansiedadEva(dto.getAnsiedadEva())
                .medicamentosActuales(dto.getMedicamentosActuales())
                .alergiasReferidas(dto.getAlergiasReferidas())
                .build();
        q = cuestionarioPreconsultaRepository.save(q);
        return toDto(q);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CuestionarioPreconsultaDto> getByCitaId(Long citaId) {
        return cuestionarioPreconsultaRepository.findByCitaId(citaId).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CuestionarioPreconsultaDto> getByConsultaId(Long consultaId) {
        Consulta consulta = consultaRepository.findById(consultaId).orElse(null);
        if (consulta == null || consulta.getCita() == null) return Optional.empty();
        return getByCitaId(consulta.getCita().getId());
    }

    private CuestionarioPreconsultaDto toDto(CuestionarioPreconsulta q) {
        return CuestionarioPreconsultaDto.builder()
                .id(q.getId())
                .citaId(q.getCita() != null ? q.getCita().getId() : null)
                .pacienteId(q.getPaciente() != null ? q.getPaciente().getId() : null)
                .motivoPalabras(q.getMotivoPalabras())
                .dolorEva(q.getDolorEva())
                .ansiedadEva(q.getAnsiedadEva())
                .medicamentosActuales(q.getMedicamentosActuales())
                .alergiasReferidas(q.getAlergiasReferidas())
                .enviadoAt(q.getEnviadoAt())
                .createdAt(q.getCreatedAt())
                .build();
    }
}
