/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.SignosVitalesUrgenciaDto;
import com.sesa.salud.dto.SignosVitalesUrgenciaRequestDto;
import com.sesa.salud.entity.SignosVitalesUrgencia;
import com.sesa.salud.entity.UrgenciaRegistro;
import com.sesa.salud.repository.SignosVitalesUrgenciaRepository;
import com.sesa.salud.repository.UrgenciaRegistroRepository;
import com.sesa.salud.service.SignosVitalesUrgenciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SignosVitalesUrgenciaServiceImpl implements SignosVitalesUrgenciaService {

    private final SignosVitalesUrgenciaRepository signosVitalesUrgenciaRepository;
    private final UrgenciaRegistroRepository urgenciaRegistroRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SignosVitalesUrgenciaDto> findByUrgenciaRegistroId(Long urgenciaRegistroId) {
        return signosVitalesUrgenciaRepository.findByUrgenciaRegistroIdOrderByFechaHoraAsc(urgenciaRegistroId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SignosVitalesUrgenciaDto create(SignosVitalesUrgenciaRequestDto dto) {
        UrgenciaRegistro urgencia = urgenciaRegistroRepository.findById(dto.getUrgenciaRegistroId())
                .orElseThrow(() -> new RuntimeException("Urgencia no encontrada: " + dto.getUrgenciaRegistroId()));
        LocalDateTime fechaHora = dto.getFechaHora() != null ? dto.getFechaHora() : LocalDateTime.now();
        SignosVitalesUrgencia sv = SignosVitalesUrgencia.builder()
                .urgenciaRegistro(urgencia)
                .fechaHora(fechaHora)
                .presionArterial(dto.getPresionArterial())
                .frecuenciaCardiaca(dto.getFrecuenciaCardiaca())
                .frecuenciaRespiratoria(dto.getFrecuenciaRespiratoria())
                .temperatura(dto.getTemperatura())
                .saturacionO2(dto.getSaturacionO2())
                .peso(dto.getPeso())
                .dolorEva(dto.getDolorEva())
                .glasgowOcular(dto.getGlasgowOcular())
                .glasgowVerbal(dto.getGlasgowVerbal())
                .glasgowMotor(dto.getGlasgowMotor())
                .build();
        sv = signosVitalesUrgenciaRepository.save(sv);
        return toDto(sv);
    }

    private SignosVitalesUrgenciaDto toDto(SignosVitalesUrgencia sv) {
        return SignosVitalesUrgenciaDto.builder()
                .id(sv.getId())
                .urgenciaRegistroId(sv.getUrgenciaRegistro().getId())
                .fechaHora(sv.getFechaHora())
                .presionArterial(sv.getPresionArterial())
                .frecuenciaCardiaca(sv.getFrecuenciaCardiaca())
                .frecuenciaRespiratoria(sv.getFrecuenciaRespiratoria())
                .temperatura(sv.getTemperatura())
                .saturacionO2(sv.getSaturacionO2())
                .peso(sv.getPeso())
                .dolorEva(sv.getDolorEva())
                .glasgowOcular(sv.getGlasgowOcular())
                .glasgowVerbal(sv.getGlasgowVerbal())
                .glasgowMotor(sv.getGlasgowMotor())
                .createdAt(sv.getCreatedAt())
                .build();
    }
}
