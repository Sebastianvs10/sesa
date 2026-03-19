/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.*;
import com.sesa.salud.entity.*;
import com.sesa.salud.repository.*;
import com.sesa.salud.security.JwtPrincipal;
import com.sesa.salud.service.AtencionService;
import com.sesa.salud.service.HistoriaClinicaService;
import com.sesa.salud.service.UrgenciaRegistroService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UrgenciaRegistroServiceImpl implements UrgenciaRegistroService {

    /** Límites de espera en minutos por nivel de triage (Res. 5596/2015). */
    private static final Map<String, Integer> TRIAGE_LIMITE_MINUTOS = Map.of(
            "I", 0, "II", 30, "III", 60, "IV", 120, "V", 240
    );
    private static final List<String> ESTADOS = List.of("EN_ESPERA", "EN_ATENCION", "ALTA");
    private static final List<String> TRIAGES = List.of("I", "II", "III", "IV", "V");

    private final UrgenciaRegistroRepository urgenciaRegistroRepository;
    private final PacienteRepository pacienteRepository;
    private final AtencionRepository atencionRepository;
    private final PersonalRepository personalRepository;
    private final HistoriaClinicaService historiaClinicaService;
    private final AtencionService atencionService;

    @Override
    @Transactional(readOnly = true)
    public Page<UrgenciaRegistroDto> findAll(Pageable pageable) {
        return urgenciaRegistroRepository.findAll(pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UrgenciaRegistroDto> findByEstado(String estado, Pageable pageable) {
        return urgenciaRegistroRepository.findByEstado(estado, pageable).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UrgenciaRegistroDto findById(Long id) {
        UrgenciaRegistro u = urgenciaRegistroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro de urgencia no encontrado: " + id));
        return toDto(u);
    }

    @Override
    @Transactional
    public UrgenciaRegistroDto create(UrgenciaRegistroRequestDto dto) {
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + dto.getPacienteId()));
        Personal profesionalTriage = dto.getProfesionalTriageId() != null
                ? personalRepository.findById(dto.getProfesionalTriageId()).orElse(null)
                : null;
        UrgenciaRegistro u = UrgenciaRegistro.builder()
                .paciente(paciente)
                .nivelTriage(dto.getNivelTriage())
                .estado(dto.getEstado() != null ? dto.getEstado() : "EN_ESPERA")
                .fechaHoraIngreso(LocalDateTime.now())
                .observaciones(dto.getObservaciones())
                .tipoLlegada(dto.getTipoLlegada())
                .motivoConsulta(dto.getMotivoConsulta())
                .profesionalTriage(profesionalTriage)
                .svPresionArterial(dto.getSvPresionArterial())
                .svFrecuenciaCardiaca(dto.getSvFrecuenciaCardiaca())
                .svFrecuenciaRespiratoria(dto.getSvFrecuenciaRespiratoria())
                .svTemperatura(dto.getSvTemperatura())
                .svSaturacionO2(dto.getSvSaturacionO2())
                .svPeso(dto.getSvPeso())
                .svDolorEva(dto.getSvDolorEva())
                .glasgowOcular(dto.getGlasgowOcular())
                .glasgowVerbal(dto.getGlasgowVerbal())
                .glasgowMotor(dto.getGlasgowMotor())
                .build();
        u = urgenciaRegistroRepository.save(u);

        // Vinculación automática: si el paciente no tiene HC, crearla; luego crear Atención para poder registrar evoluciones (sugerencia 1).
        Personal profesionalAtencion = profesionalTriage != null ? profesionalTriage : getCurrentUserPersonal();
        Optional<HistoriaClinicaDto> hcOpt = historiaClinicaService.findByPacienteId(paciente.getId());
        if (hcOpt.isEmpty()) {
            try {
                HistoriaClinicaDto created = historiaClinicaService.createForPaciente(paciente.getId(), new HistoriaClinicaRequestDto());
                hcOpt = Optional.of(created);
            } catch (Exception e) {
                // Paciente ya tiene HC creada en paralelo
                hcOpt = historiaClinicaService.findByPacienteId(paciente.getId());
            }
        }
        if (hcOpt.isPresent() && profesionalAtencion != null) {
            try {
                AtencionRequestDto areq = new AtencionRequestDto();
                areq.setHistoriaId(hcOpt.get().getId());
                areq.setProfesionalId(profesionalAtencion.getId());
                areq.setMotivoConsulta(dto.getMotivoConsulta());
                areq.setFechaAtencion(Instant.now());
                AtencionDto atencionCreada = atencionService.create(areq);
                u.setAtencion(atencionRepository.findById(atencionCreada.getId()).orElse(null));
                u = urgenciaRegistroRepository.save(u);
            } catch (Exception e) {
                // No bloquear el ingreso si falla la creación de atención
            }
        }
        return toDto(u);
    }

    /** Obtiene el Personal asociado al usuario autenticado (para vincular atención cuando no hay profesional de triage). */
    private Personal getCurrentUserPersonal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof JwtPrincipal principal)) {
            return null;
        }
        return personalRepository.findByUsuario_Id(principal.userId()).orElse(null);
    }

    @Override
    @Transactional
    public UrgenciaRegistroDto update(Long id, UrgenciaRegistroRequestDto dto) {
        UrgenciaRegistro u = urgenciaRegistroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro de urgencia no encontrado: " + id));
        u.setNivelTriage(dto.getNivelTriage());
        u.setEstado(dto.getEstado());
        u.setObservaciones(dto.getObservaciones());
        u = urgenciaRegistroRepository.save(u);
        return toDto(u);
    }

    @Override
    @Transactional
    public UrgenciaRegistroDto cambiarEstado(Long id, String nuevoEstado) {
        UrgenciaRegistro u = urgenciaRegistroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro de urgencia no encontrado: " + id));
        u.setEstado(nuevoEstado);
        if ("EN_ATENCION".equalsIgnoreCase(nuevoEstado) && u.getFechaHoraInicioAtencion() == null) {
            u.setFechaHoraInicioAtencion(LocalDateTime.now());
        }
        return toDto(urgenciaRegistroRepository.save(u));
    }

    @Override
    @Transactional
    public UrgenciaRegistroDto updateTriage(Long id, UrgenciaTriagePatchDto dto) {
        UrgenciaRegistro u = urgenciaRegistroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro de urgencia no encontrado: " + id));
        if (dto.getNivelTriage() != null && !dto.getNivelTriage().isBlank()) {
            u.setNivelTriage(dto.getNivelTriage());
        }
        if (dto.getProfesionalTriageId() != null) {
            u.setProfesionalTriage(personalRepository.findById(dto.getProfesionalTriageId()).orElse(u.getProfesionalTriage()));
        } else {
            Personal current = getCurrentUserPersonal();
            if (current != null) {
                u.setProfesionalTriage(current);
            }
        }
        return toDto(urgenciaRegistroRepository.save(u));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (!urgenciaRegistroRepository.existsById(id)) {
            throw new RuntimeException("Registro de urgencia no encontrado: " + id);
        }
        urgenciaRegistroRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public UrgenciaDashboardDto getDashboard() {
        Map<String, Long> conteoPorEstado = new LinkedHashMap<>();
        for (String e : ESTADOS) {
            conteoPorEstado.put(e, urgenciaRegistroRepository.countByEstado(e));
        }
        Map<String, Long> conteoPorTriage = new LinkedHashMap<>();
        for (String t : TRIAGES) {
            conteoPorTriage.put(t, urgenciaRegistroRepository.countByNivelTriage(t));
        }
        LocalDateTime now = LocalDateTime.now();
        List<UrgenciaRegistro> enEspera = urgenciaRegistroRepository.findByEstadoOrderByFechaHoraIngresoAsc(
                "EN_ESPERA", PageRequest.of(0, 500));
        List<UrgenciaFueraDeTiempoItemDto> fueraDeTiempo = new ArrayList<>();
        long sumaMinutos = 0;
        for (UrgenciaRegistro u : enEspera) {
            long minutos = java.time.Duration.between(u.getFechaHoraIngreso(), now).toMinutes();
            sumaMinutos += minutos;
            int limite = TRIAGE_LIMITE_MINUTOS.getOrDefault(u.getNivelTriage() != null ? u.getNivelTriage().trim().toUpperCase() : "V", 240);
            if (minutos > limite) {
                String nombre = u.getPaciente().getNombres() + " " + (u.getPaciente().getApellidos() != null ? u.getPaciente().getApellidos() : "");
                fueraDeTiempo.add(UrgenciaFueraDeTiempoItemDto.builder()
                        .id(u.getId())
                        .pacienteId(u.getPaciente().getId())
                        .pacienteNombre(nombre.trim())
                        .nivelTriage(u.getNivelTriage())
                        .fechaHoraIngreso(u.getFechaHoraIngreso())
                        .minutosEspera(minutos)
                        .limiteMinutos(limite)
                        .build());
            }
        }
        double tiempoPromedio = enEspera.isEmpty() ? 0.0 : (double) sumaMinutos / enEspera.size();
        return UrgenciaDashboardDto.builder()
                .conteoPorEstado(conteoPorEstado)
                .conteoPorTriage(conteoPorTriage)
                .fueraDeTiempo(fueraDeTiempo)
                .tiempoPromedioEsperaMinutos(tiempoPromedio)
                .totalEnEspera(enEspera.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UrgenciaReporteCumplimientoDto getReporteCumplimiento(LocalDate desde, LocalDate hasta) {
        LocalDateTime desdeDt = desde.atStartOfDay();
        LocalDateTime hastaDt = hasta.atTime(LocalTime.MAX);
        List<UrgenciaRegistro> registros = urgenciaRegistroRepository.findByFechaHoraIngresoBetweenOrderByFechaHoraIngresoAsc(desdeDt, hastaDt);
        long totalAtendidos = 0;
        long totalDentroTiempo = 0;
        long totalFueraTiempo = 0;
        Map<String, long[]> porTriage = new LinkedHashMap<>();
        for (String t : TRIAGES) {
            porTriage.put(t, new long[]{0, 0, 0}); // total, dentro, fuera
        }
        for (UrgenciaRegistro u : registros) {
            if (u.getFechaHoraInicioAtencion() == null) continue;
            long minutosEspera = java.time.Duration.between(u.getFechaHoraIngreso(), u.getFechaHoraInicioAtencion()).toMinutes();
            int limite = TRIAGE_LIMITE_MINUTOS.getOrDefault(u.getNivelTriage() != null ? u.getNivelTriage().trim().toUpperCase() : "V", 240);
            totalAtendidos++;
            boolean dentro = minutosEspera <= limite;
            if (dentro) totalDentroTiempo++; else totalFueraTiempo++;
            String triage = u.getNivelTriage() != null ? u.getNivelTriage().trim().toUpperCase() : "V";
            if (!TRIAGES.contains(triage)) triage = "V";
            long[] arr = porTriage.get(triage);
            arr[0]++;
            if (dentro) arr[1]++; else arr[2]++;
        }
        List<CumplimientoTriageDto> porTriageList = new ArrayList<>();
        for (String t : TRIAGES) {
            long[] arr = porTriage.get(t);
            double pct = arr[0] == 0 ? 100.0 : (100.0 * arr[1] / arr[0]);
            porTriageList.add(CumplimientoTriageDto.builder()
                    .nivelTriage(t)
                    .total(arr[0])
                    .dentroTiempo(arr[1])
                    .fueraTiempo(arr[2])
                    .porcentajeCumplimiento(pct)
                    .build());
        }
        double porcentajeGlobal = totalAtendidos == 0 ? 100.0 : (100.0 * totalDentroTiempo / totalAtendidos);
        return UrgenciaReporteCumplimientoDto.builder()
                .desde(desde)
                .hasta(hasta)
                .porTriage(porTriageList)
                .totalRegistros(registros.size())
                .totalAtendidos(totalAtendidos)
                .totalDentroTiempo(totalDentroTiempo)
                .totalFueraTiempo(totalFueraTiempo)
                .porcentajeCumplimiento(porcentajeGlobal)
                .build();
    }

    @Override
    @Transactional
    public UrgenciaRegistroDto darAlta(Long id, AltaReferenciaRequestDto request) {
        UrgenciaRegistro u = urgenciaRegistroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro de urgencia no encontrado: " + id));
        u.setEstado("ALTA");
        if (request != null) {
            u.setAltaDiagnostico(request.getDiagnostico());
            u.setAltaTratamiento(request.getTratamiento());
            u.setAltaRecomendaciones(request.getRecomendaciones());
            u.setAltaProximaCita(request.getProximaCita());
        }
        return toDto(urgenciaRegistroRepository.save(u));
    }

    private UrgenciaRegistroDto toDto(UrgenciaRegistro u) {
        String pacienteNombre = u.getPaciente().getNombres() + " " +
                (u.getPaciente().getApellidos() != null ? u.getPaciente().getApellidos() : "");
        String profesionalTriageNombre = u.getProfesionalTriage() != null
                ? (u.getProfesionalTriage().getNombres() + " " + (u.getProfesionalTriage().getApellidos() != null ? u.getProfesionalTriage().getApellidos() : "")).trim()
                : null;
        return UrgenciaRegistroDto.builder()
                .id(u.getId())
                .pacienteId(u.getPaciente().getId())
                .pacienteNombre(pacienteNombre.trim())
                .pacienteDocumento(u.getPaciente().getDocumento())
                .nivelTriage(u.getNivelTriage())
                .estado(u.getEstado())
                .fechaHoraIngreso(u.getFechaHoraIngreso())
                .observaciones(u.getObservaciones())
                .atencionId(u.getAtencion() != null ? u.getAtencion().getId() : null)
                .createdAt(u.getCreatedAt())
                .tipoLlegada(u.getTipoLlegada())
                .motivoConsulta(u.getMotivoConsulta())
                .profesionalTriageId(u.getProfesionalTriage() != null ? u.getProfesionalTriage().getId() : null)
                .profesionalTriageNombre(profesionalTriageNombre)
                .svPresionArterial(u.getSvPresionArterial())
                .svFrecuenciaCardiaca(u.getSvFrecuenciaCardiaca())
                .svFrecuenciaRespiratoria(u.getSvFrecuenciaRespiratoria())
                .svTemperatura(u.getSvTemperatura())
                .svSaturacionO2(u.getSvSaturacionO2())
                .svPeso(u.getSvPeso())
                .svDolorEva(u.getSvDolorEva())
                .glasgowOcular(u.getGlasgowOcular())
                .glasgowVerbal(u.getGlasgowVerbal())
                .glasgowMotor(u.getGlasgowMotor())
                .altaDiagnostico(u.getAltaDiagnostico())
                .altaTratamiento(u.getAltaTratamiento())
                .altaRecomendaciones(u.getAltaRecomendaciones())
                .altaProximaCita(u.getAltaProximaCita())
                .build();
    }
}
