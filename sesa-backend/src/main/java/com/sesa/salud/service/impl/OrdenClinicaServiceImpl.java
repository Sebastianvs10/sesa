/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.OrdenClinicaBatchRequestDto;
import com.sesa.salud.dto.OrdenClinicaDto;
import com.sesa.salud.dto.OrdenClinicaItemDto;
import com.sesa.salud.dto.OrdenClinicaRequestDto;
import com.sesa.salud.dto.ResultadoOrdenDto;
import com.sesa.salud.entity.Consulta;
import com.sesa.salud.entity.OrdenClinica;
import com.sesa.salud.entity.OrdenClinicaItem;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.entity.Personal;
import com.sesa.salud.entity.ResultadoCriticoLectura;
import com.sesa.salud.repository.ConsultaRepository;
import com.sesa.salud.repository.OrdenClinicaRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.repository.PersonalRepository;
import com.sesa.salud.repository.ResultadoCriticoLecturaRepository;
import com.sesa.salud.security.JwtPrincipal;
import com.sesa.salud.service.OrdenClinicaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrdenClinicaServiceImpl implements OrdenClinicaService {

    private final OrdenClinicaRepository ordenClinicaRepository;
    private final PacienteRepository pacienteRepository;
    private final ConsultaRepository consultaRepository;
    private final PersonalRepository personalRepository;
    private final ResultadoCriticoLecturaRepository resultadoCriticoLecturaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OrdenClinicaDto> findByPacienteId(Long pacienteId, Pageable pageable) {
        return ordenClinicaRepository.findByPaciente_IdOrderByCreatedAtDesc(pacienteId, pageable)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenClinicaDto> findByTipo(String tipo, Pageable pageable) {
        return ordenClinicaRepository.findByTipoOrderByCreatedAtDesc(tipo, pageable)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrdenClinicaDto findById(Long id) {
        return toDto(ordenClinicaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden clínica no encontrada: " + id)));
    }

    @Override
    @Transactional
    public OrdenClinicaDto create(OrdenClinicaRequestDto dto) {
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + dto.getPacienteId()));
        Consulta consulta = consultaRepository.findById(dto.getConsultaId())
                .orElseThrow(() -> new RuntimeException("Consulta no encontrada: " + dto.getConsultaId()));

        OrdenClinica orden = OrdenClinica.builder()
                .paciente(paciente)
                .consulta(consulta)
                .tipo(dto.getTipo())
                .detalle(dto.getDetalle())
                .cantidadPrescrita(dto.getCantidadPrescrita())
                .unidadMedida(dto.getUnidadMedida())
                .frecuencia(dto.getFrecuencia())
                .duracionDias(dto.getDuracionDias())
                .estado(dto.getEstado() != null ? dto.getEstado() : "PENDIENTE")
                .valorEstimado(dto.getValorEstimado())
                .build();
        return toDto(ordenClinicaRepository.save(orden));
    }

    @Override
    @Transactional
    public OrdenClinicaDto createBatch(OrdenClinicaBatchRequestDto batch) {
        Paciente paciente = pacienteRepository.findById(batch.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + batch.getPacienteId()));
        Consulta consulta = consultaRepository.findById(batch.getConsultaId())
                .orElseThrow(() -> new RuntimeException("Consulta no encontrada: " + batch.getConsultaId()));

        OrdenClinica orden = OrdenClinica.builder()
                .paciente(paciente)
                .consulta(consulta)
                .tipo("COMPUESTA")
                .detalle(null)
                .estado("PENDIENTE")
                .items(new ArrayList<>())
                .build();
        orden = ordenClinicaRepository.save(orden);

        int index = 0;
        for (var itemDto : batch.getItems()) {
            OrdenClinicaItem item = OrdenClinicaItem.builder()
                    .orden(orden)
                    .tipo(itemDto.getTipo())
                    .detalle(itemDto.getDetalle())
                    .cantidadPrescrita(itemDto.getCantidadPrescrita())
                    .unidadMedida(itemDto.getUnidadMedida())
                    .frecuencia(itemDto.getFrecuencia())
                    .duracionDias(itemDto.getDuracionDias())
                    .valorEstimado(itemDto.getValorEstimado())
                    .ordenItemIndex(index++)
                    .build();
            orden.getItems().add(item);
        }
        orden = ordenClinicaRepository.save(orden);
        return toDto(orden);
    }

    @Override
    @Transactional
    public OrdenClinicaDto update(Long id, OrdenClinicaRequestDto dto) {
        OrdenClinica orden = ordenClinicaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden clínica no encontrada: " + id));
        if (dto.getTipo() != null) orden.setTipo(dto.getTipo());
        if (dto.getDetalle() != null) orden.setDetalle(dto.getDetalle());
        if (dto.getCantidadPrescrita() != null) orden.setCantidadPrescrita(dto.getCantidadPrescrita());
        if (dto.getUnidadMedida() != null) orden.setUnidadMedida(dto.getUnidadMedida());
        if (dto.getFrecuencia() != null) orden.setFrecuencia(dto.getFrecuencia());
        if (dto.getDuracionDias() != null) orden.setDuracionDias(dto.getDuracionDias());
        if (dto.getEstado() != null) orden.setEstado(dto.getEstado());
        if (dto.getValorEstimado() != null) orden.setValorEstimado(dto.getValorEstimado());
        return toDto(ordenClinicaRepository.save(orden));
    }

    @Override
    @Transactional
    public OrdenClinicaDto registrarResultado(Long id, ResultadoOrdenDto dto) {
        OrdenClinica orden = ordenClinicaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden clínica no encontrada: " + id));
        orden.setResultado(dto.getResultado());
        orden.setFechaResultado(Instant.now());
        orden.setEstado("COMPLETADO");
        orden.setResultadoCritico(Boolean.TRUE.equals(dto.getResultadoCritico()));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtPrincipal principal) {
            personalRepository.findByUsuario_Id(principal.userId()).ifPresent(orden::setResultadoRegistradoPor);
        }
        return toDto(ordenClinicaRepository.save(orden));
    }

    @Override
    @Transactional
    public void marcarResultadoLeido(Long id) {
        OrdenClinica orden = ordenClinicaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden clínica no encontrada: " + id));
        if (!Boolean.TRUE.equals(orden.getResultadoCritico())) {
            return;
        }
        java.util.Optional<Long> personalIdOpt = getCurrentPersonalId();
        if (personalIdOpt.isEmpty()) {
            throw new RuntimeException("Usuario sin personal asociado para registrar lectura");
        }
        Long personalId = personalIdOpt.get();
        if (resultadoCriticoLecturaRepository.existsByOrdenClinica_IdAndPersonal_Id(id, personalId)) {
            return;
        }
        Personal personal = personalRepository.findById(personalId)
                .orElseThrow(() -> new RuntimeException("Personal no encontrado: " + personalId));
        ResultadoCriticoLectura lectura = ResultadoCriticoLectura.builder()
                .ordenClinica(orden)
                .personal(personal)
                .leidoAt(Instant.now())
                .build();
        resultadoCriticoLecturaRepository.save(lectura);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (!ordenClinicaRepository.existsById(id)) {
            throw new RuntimeException("Orden clínica no encontrada: " + id);
        }
        ordenClinicaRepository.deleteById(id);
    }

    private OrdenClinicaDto toDto(OrdenClinica o) {
        String pacienteNombre = o.getPaciente().getNombres() + " " +
                (o.getPaciente().getApellidos() != null ? o.getPaciente().getApellidos() : "");
        String regPorNombre = null;
        String regPorRol = null;
        if (o.getResultadoRegistradoPor() != null) {
            var p = o.getResultadoRegistradoPor();
            regPorNombre = (p.getNombres() + " " + (p.getApellidos() != null ? p.getApellidos() : "")).trim();
            regPorRol = p.getRol();
        }
        List<OrdenClinicaItemDto> itemsDto = new ArrayList<>();
        if (o.getItems() != null && !o.getItems().isEmpty()) {
            for (OrdenClinicaItem it : o.getItems()) {
                itemsDto.add(OrdenClinicaItemDto.builder()
                        .id(it.getId())
                        .tipo(it.getTipo())
                        .detalle(it.getDetalle())
                        .cantidadPrescrita(it.getCantidadPrescrita())
                        .unidadMedida(it.getUnidadMedida())
                        .frecuencia(it.getFrecuencia())
                        .duracionDias(it.getDuracionDias())
                        .valorEstimado(it.getValorEstimado())
                        .build());
            }
        } else {
            // Órden legacy de un solo ítem (datos en cabecera)
            itemsDto.add(OrdenClinicaItemDto.builder()
                    .id(null)
                    .tipo(o.getTipo())
                    .detalle(o.getDetalle())
                    .cantidadPrescrita(o.getCantidadPrescrita())
                    .unidadMedida(o.getUnidadMedida())
                    .frecuencia(o.getFrecuencia())
                    .duracionDias(o.getDuracionDias())
                    .valorEstimado(o.getValorEstimado())
                    .build());
        }
        Boolean resultadoCritico = o.getResultadoCritico() != null && o.getResultadoCritico();
        Boolean leidoPorUsuarioActual = false;
        if (resultadoCritico) {
            Optional<Long> personalId = getCurrentPersonalId();
            leidoPorUsuarioActual = personalId.isPresent()
                    && resultadoCriticoLecturaRepository.existsByOrdenClinica_IdAndPersonal_Id(o.getId(), personalId.get());
        }
        return OrdenClinicaDto.builder()
                .id(o.getId())
                .pacienteId(o.getPaciente().getId())
                .pacienteNombre(pacienteNombre.trim())
                .consultaId(o.getConsulta().getId())
                .tipo(o.getTipo())
                .detalle(o.getDetalle())
                .cantidadPrescrita(o.getCantidadPrescrita())
                .unidadMedida(o.getUnidadMedida())
                .frecuencia(o.getFrecuencia())
                .duracionDias(o.getDuracionDias())
                .estado(o.getEstado())
                .resultado(o.getResultado())
                .fechaResultado(o.getFechaResultado())
                .resultadoRegistradoPorNombre(regPorNombre)
                .resultadoRegistradoPorRol(regPorRol)
                .valorEstimado(o.getValorEstimado())
                .createdAt(o.getCreatedAt())
                .items(itemsDto)
                .resultadoCritico(resultadoCritico)
                .leidoPorUsuarioActual(leidoPorUsuarioActual)
                .build();
    }

    private Optional<Long> getCurrentPersonalId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof JwtPrincipal principal)) {
            return Optional.empty();
        }
        return personalRepository.findByUsuario_Id(principal.userId()).map(Personal::getId);
    }
}
