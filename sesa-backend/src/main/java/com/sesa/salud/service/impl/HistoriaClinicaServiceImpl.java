/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.AtencionRequestDto;
import com.sesa.salud.dto.CrearHistoriaCompletaRequestDto;
import com.sesa.salud.dto.HistoriaClinicaDto;
import com.sesa.salud.dto.HistoriaClinicaRequestDto;
import com.sesa.salud.entity.HistoriaClinica;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.repository.HistoriaClinicaRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.repository.PersonalRepository;
import com.sesa.salud.security.JwtPrincipal;
import com.sesa.salud.service.AtencionService;
import com.sesa.salud.service.HistoriaClinicaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HistoriaClinicaServiceImpl implements HistoriaClinicaService {

    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final PacienteRepository pacienteRepository;
    private final PersonalRepository personalRepository;
    private final AtencionService atencionService;

    @Override
    @Transactional(readOnly = true)
    public Optional<HistoriaClinicaDto> findByPacienteId(Long pacienteId) {
        return historiaClinicaRepository.findByPacienteId(pacienteId)
                .map(this::toDto);
    }

    @Override
    @Transactional
    public HistoriaClinicaDto createForPaciente(Long pacienteId, HistoriaClinicaRequestDto dto) {
        if (historiaClinicaRepository.findByPacienteId(pacienteId).isPresent()) {
            throw new RuntimeException("El paciente ya tiene una historia clínica");
        }
        Paciente p = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + pacienteId));
        HistoriaClinica hc = HistoriaClinica.builder()
                .paciente(p)
                .grupoSanguineo(dto.getGrupoSanguineo())
                .alergiasGenerales(dto.getAlergiasGenerales())
                .antecedentesPersonales(dto.getAntecedentesPersonales())
                .antecedentesQuirurgicos(dto.getAntecedentesQuirurgicos())
                .antecedentesFarmacologicos(dto.getAntecedentesFarmacologicos())
                .antecedentesTraumaticos(dto.getAntecedentesTraumaticos())
                .antecedentesGinecoobstetricos(dto.getAntecedentesGinecoobstetricos())
                .antecedentesFamiliares(dto.getAntecedentesFamiliares())
                .habitosTabaco(dto.getHabitosTabaco())
                .habitosAlcohol(dto.getHabitosAlcohol())
                .habitosSustancias(dto.getHabitosSustancias())
                .habitosDetalles(dto.getHabitosDetalles())
                .build();
        hc = historiaClinicaRepository.save(hc);
        return toDto(hc);
    }

    @Override
    @Transactional
    public HistoriaClinicaDto createCompleta(Long pacienteId, CrearHistoriaCompletaRequestDto dto) {
        if (historiaClinicaRepository.findByPacienteId(pacienteId).isPresent()) {
            throw new RuntimeException("El paciente ya tiene una historia clínica");
        }
        var principal = (JwtPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var personalOpt = dto.getProfesionalId() != null
                ? personalRepository.findById(dto.getProfesionalId())
                : personalRepository.findByUsuario_Id(principal.userId());

        HistoriaClinicaRequestDto hcDto = new HistoriaClinicaRequestDto();
        hcDto.setGrupoSanguineo(dto.getGrupoSanguineo());
        hcDto.setAlergiasGenerales(dto.getAlergiasGenerales());
        hcDto.setAntecedentesPersonales(dto.getAntecedentesPersonales());
        hcDto.setAntecedentesQuirurgicos(dto.getAntecedentesQuirurgicos());
        hcDto.setAntecedentesFarmacologicos(dto.getAntecedentesFarmacologicos());
        hcDto.setAntecedentesTraumaticos(dto.getAntecedentesTraumaticos());
        hcDto.setAntecedentesGinecoobstetricos(dto.getAntecedentesGinecoobstetricos());
        hcDto.setAntecedentesFamiliares(dto.getAntecedentesFamiliares());
        hcDto.setHabitosTabaco(dto.getHabitosTabaco());
        hcDto.setHabitosAlcohol(dto.getHabitosAlcohol());
        hcDto.setHabitosSustancias(dto.getHabitosSustancias());
        hcDto.setHabitosDetalles(dto.getHabitosDetalles());

        HistoriaClinicaDto hcResult = createForPaciente(pacienteId, hcDto);

        if (personalOpt.isPresent()) {
            var personal = personalOpt.get();
            AtencionRequestDto atencionDto = new AtencionRequestDto();
            atencionDto.setHistoriaId(hcResult.getId());
            atencionDto.setProfesionalId(personal.getId());
            atencionDto.setMotivoConsulta(dto.getMotivoConsulta());
            atencionDto.setEnfermedadActual(dto.getEnfermedadActual());
            atencionDto.setVersionEnfermedad(dto.getVersionEnfermedad());
            atencionDto.setSintomasAsociados(dto.getSintomasAsociados());
            atencionDto.setFactoresMejoran(dto.getFactoresMejoran());
            atencionDto.setFactoresEmpeoran(dto.getFactoresEmpeoran());
            atencionDto.setRevisionSistemas(dto.getRevisionSistemas());
            atencionDto.setPresionArterial(dto.getPresionArterial());
            atencionDto.setFrecuenciaCardiaca(dto.getFrecuenciaCardiaca());
            atencionDto.setFrecuenciaRespiratoria(dto.getFrecuenciaRespiratoria());
            atencionDto.setTemperatura(dto.getTemperatura());
            atencionDto.setPeso(dto.getPeso());
            atencionDto.setTalla(dto.getTalla());
            atencionDto.setImc(dto.getImc());
            atencionDto.setEvaluacionGeneral(dto.getEvaluacionGeneral());
            atencionDto.setHallazgos(dto.getHallazgos());
            atencionDto.setDiagnostico(dto.getDiagnostico());
            atencionDto.setCodigoCie10(dto.getCodigoCie10());
            atencionDto.setPlanTratamiento(dto.getPlanTratamiento());
            atencionDto.setTratamientoFarmacologico(dto.getTratamientoFarmacologico());
            atencionDto.setOrdenesMedicas(dto.getOrdenesMedicas());
            atencionDto.setExamenesSolicitados(dto.getExamenesSolicitados());
            atencionDto.setIncapacidad(dto.getIncapacidad());
            atencionDto.setRecomendaciones(dto.getRecomendaciones());
            atencionService.create(atencionDto);
        }
        return hcResult;
    }

    @Override
    @Transactional
    public HistoriaClinicaDto update(Long id, HistoriaClinicaRequestDto dto) {
        HistoriaClinica hc = historiaClinicaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Historia clínica no encontrada: " + id));
        if (dto.getEstado() != null && !dto.getEstado().isBlank()) hc.setEstado(dto.getEstado());
        if (dto.getGrupoSanguineo() != null) hc.setGrupoSanguineo(dto.getGrupoSanguineo());
        if (dto.getAlergiasGenerales() != null) hc.setAlergiasGenerales(dto.getAlergiasGenerales());
        if (dto.getAntecedentesPersonales() != null) hc.setAntecedentesPersonales(dto.getAntecedentesPersonales());
        if (dto.getAntecedentesQuirurgicos() != null) hc.setAntecedentesQuirurgicos(dto.getAntecedentesQuirurgicos());
        if (dto.getAntecedentesFarmacologicos() != null) hc.setAntecedentesFarmacologicos(dto.getAntecedentesFarmacologicos());
        if (dto.getAntecedentesTraumaticos() != null) hc.setAntecedentesTraumaticos(dto.getAntecedentesTraumaticos());
        if (dto.getAntecedentesGinecoobstetricos() != null) hc.setAntecedentesGinecoobstetricos(dto.getAntecedentesGinecoobstetricos());
        if (dto.getAntecedentesFamiliares() != null) hc.setAntecedentesFamiliares(dto.getAntecedentesFamiliares());
        if (dto.getHabitosTabaco() != null) hc.setHabitosTabaco(dto.getHabitosTabaco());
        if (dto.getHabitosAlcohol() != null) hc.setHabitosAlcohol(dto.getHabitosAlcohol());
        if (dto.getHabitosSustancias() != null) hc.setHabitosSustancias(dto.getHabitosSustancias());
        if (dto.getHabitosDetalles() != null) hc.setHabitosDetalles(dto.getHabitosDetalles());
        hc = historiaClinicaRepository.save(hc);
        return toDto(hc);
    }

    private HistoriaClinicaDto toDto(HistoriaClinica hc) {
        Paciente p = hc.getPaciente();
        return HistoriaClinicaDto.builder()
                .id(hc.getId())
                .pacienteId(p.getId())
                .pacienteNombre(p.getNombres() + " " + (p.getApellidos() != null ? p.getApellidos() : ""))
                .pacienteDocumento(p.getDocumento())
                .fechaApertura(hc.getFechaApertura())
                .estado(hc.getEstado())
                .grupoSanguineo(hc.getGrupoSanguineo())
                .alergiasGenerales(hc.getAlergiasGenerales())
                .antecedentesPersonales(hc.getAntecedentesPersonales())
                .antecedentesQuirurgicos(hc.getAntecedentesQuirurgicos())
                .antecedentesFarmacologicos(hc.getAntecedentesFarmacologicos())
                .antecedentesTraumaticos(hc.getAntecedentesTraumaticos())
                .antecedentesGinecoobstetricos(hc.getAntecedentesGinecoobstetricos())
                .antecedentesFamiliares(hc.getAntecedentesFamiliares())
                .habitosTabaco(hc.getHabitosTabaco())
                .habitosAlcohol(hc.getHabitosAlcohol())
                .habitosSustancias(hc.getHabitosSustancias())
                .habitosDetalles(hc.getHabitosDetalles())
                .build();
    }
}
