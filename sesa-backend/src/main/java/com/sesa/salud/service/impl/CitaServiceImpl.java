/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.CitaDto;
import com.sesa.salud.dto.CitaRequestDto;
import com.sesa.salud.dto.ConsultaMedicaDto;
import com.sesa.salud.dto.ConsultasStatsDto;
import com.sesa.salud.entity.Cita;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.entity.Personal;
import com.sesa.salud.repository.CitaRepository;
import com.sesa.salud.repository.FacturaRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.repository.PersonalRepository;
import com.sesa.salud.service.CitaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CitaServiceImpl implements CitaService {

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final PersonalRepository personalRepository;
    private final FacturaRepository facturaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CitaDto> findAll(Pageable pageable) {
        return citaRepository.findAll(pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaDto> findByFecha(LocalDate fecha) {
        return citaRepository.findByFecha(fecha).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaDto> findByPacienteId(Long pacienteId, Pageable pageable) {
        return citaRepository.findByPaciente_Id(pacienteId, pageable).getContent().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CitaDto findById(Long id) {
        Cita c = citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada: " + id));
        return toDto(c);
    }

    @Override
    @Transactional
    public CitaDto create(CitaRequestDto dto) {
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + dto.getPacienteId()));
        Personal profesional = personalRepository.findById(dto.getProfesionalId())
                .orElseThrow(() -> new RuntimeException("Personal no encontrado: " + dto.getProfesionalId()));
        Cita c = Cita.builder()
                .paciente(paciente)
                .profesional(profesional)
                .servicio(dto.getServicio())
                .fechaHora(dto.getFechaHora())
                .estado(dto.getEstado() != null ? dto.getEstado() : "AGENDADA")
                .notas(dto.getNotas())
                .tipoCita(dto.getTipoCita())
                .numeroAutorizacionEps(dto.getNumeroAutorizacionEps())
                .duracionEstimadaMin(dto.getDuracionEstimadaMin())
                .build();
        c = citaRepository.save(c);
        return toDto(c);
    }

    @Override
    @Transactional
    public CitaDto update(Long id, CitaRequestDto dto) {
        Cita c = citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada: " + id));
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + dto.getPacienteId()));
        Personal profesional = personalRepository.findById(dto.getProfesionalId())
                .orElseThrow(() -> new RuntimeException("Personal no encontrado: " + dto.getProfesionalId()));
        c.setPaciente(paciente);
        c.setProfesional(profesional);
        c.setServicio(dto.getServicio());
        c.setFechaHora(dto.getFechaHora());
        c.setEstado(dto.getEstado());
        c.setNotas(dto.getNotas());
        c.setTipoCita(dto.getTipoCita());
        c.setNumeroAutorizacionEps(dto.getNumeroAutorizacionEps());
        c.setDuracionEstimadaMin(dto.getDuracionEstimadaMin());
        c = citaRepository.save(c);
        return toDto(c);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (!citaRepository.existsById(id)) {
            throw new RuntimeException("Cita no encontrada: " + id);
        }
        citaRepository.deleteById(id);
    }

    // ── Módulo Consulta Médica ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ConsultaMedicaDto> findConsultasMedicas(Long profesionalId, LocalDate fecha) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now();
        List<Cita> citas = citaRepository.findByFechaAndProfesionalId(dia, profesionalId);
        return citas.stream().map(this::toConsultaDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultaMedicaDto> findConsultasMedicasTodas(LocalDate fecha) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now();
        List<Cita> citas = citaRepository.findByFecha(dia);
        return citas.stream().map(this::toConsultaDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultasStatsDto getStatsDelDia(Long profesionalId, LocalDate fecha) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now();
        List<Object[]> rows = profesionalId != null
                ? citaRepository.countByEstadoAndFechaAndProfesionalId(dia, profesionalId)
                : citaRepository.countByEstadoAndFecha(dia);
        Map<String, Long> conteos = rows.stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1]));
        long agendadas  = conteos.getOrDefault("AGENDADA", 0L);
        long atendidas  = conteos.getOrDefault("ATENDIDA", 0L);
        long canceladas = conteos.getOrDefault("CANCELADA", 0L);
        long total      = agendadas + atendidas + canceladas;
        int pct = total > 0 ? (int) Math.round((atendidas * 100.0) / total) : 0;
        return ConsultasStatsDto.builder()
                .total(total)
                .agendadas(agendadas)
                .atendidas(atendidas)
                .canceladas(canceladas)
                .porcentajeAsistencia(pct)
                .build();
    }

    @Override
    @Transactional
    public CitaDto cancelarCita(Long id, String motivo) {
        Cita c = citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada: " + id));
        c.setEstado("CANCELADA");
        c.setMotivoCancelacion(motivo);
        c = citaRepository.save(c);
        return toDto(c);
    }

    @Override
    @Transactional
    public CitaDto cambiarEstado(Long id, String nuevoEstado) {
        Cita c = citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada: " + id));
        c.setEstado(nuevoEstado);
        c = citaRepository.save(c);
        return toDto(c);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Personal> findProfesionalesMedicos() {
        return personalRepository.findByRolIn(List.of("MEDICO", "JEFE_ENFERMERIA", "ODONTOLOGO", "PSICOLOGO", "COORDINADOR_MEDICO"));
    }

    // ── Conversores ──────────────────────────────────────────────────────

    private CitaDto toDto(Cita c) {
        String pacienteNombre = c.getPaciente().getNombres() + " " + (c.getPaciente().getApellidos() != null ? c.getPaciente().getApellidos() : "");
        String profesionalNombre = c.getProfesional().getNombres() + " " + (c.getProfesional().getApellidos() != null ? c.getProfesional().getApellidos() : "");

        // Indicador de oportunidad Res. 2953/2014
        Long diasEspera = null;
        Boolean alertaOportunidad = null;
        if (c.getCreatedAt() != null && c.getFechaHora() != null) {
            LocalDateTime creacion = LocalDateTime.ofInstant(c.getCreatedAt(), java.time.ZoneId.systemDefault());
            diasEspera = ChronoUnit.DAYS.between(creacion, c.getFechaHora());
            int limiteMaximo = "PRIMERA_VEZ".equals(c.getTipoCita()) ? 3 : 15;
            alertaOportunidad = diasEspera > limiteMaximo;
        }

        return CitaDto.builder()
                .id(c.getId())
                .pacienteId(c.getPaciente().getId())
                .pacienteNombre(pacienteNombre.trim())
                .profesionalId(c.getProfesional().getId())
                .profesionalNombre(profesionalNombre.trim())
                .servicio(c.getServicio())
                .fechaHora(c.getFechaHora())
                .estado(c.getEstado())
                .notas(c.getNotas())
                .createdAt(c.getCreatedAt())
                .tipoCita(c.getTipoCita())
                .numeroAutorizacionEps(c.getNumeroAutorizacionEps())
                .duracionEstimadaMin(c.getDuracionEstimadaMin())
                .diasEspera(diasEspera)
                .alertaOportunidad(alertaOportunidad)
                .build();
    }

    private ConsultaMedicaDto toConsultaDto(Cita c) {
        Paciente p = c.getPaciente();
        Personal prof = c.getProfesional();

        String nombreCompleto = (p.getNombres() + " " + (p.getApellidos() != null ? p.getApellidos() : "")).trim();
        String nombreProf = (prof.getNombres() + " " + (prof.getApellidos() != null ? prof.getApellidos() : "")).trim();
        String epsNombre = (p.getEps() != null) ? p.getEps().getNombre() : null;
        String epsCodigo = (p.getEps() != null) ? p.getEps().getCodigo() : null;

        Integer edad = null;
        if (p.getFechaNacimiento() != null) {
            edad = Period.between(p.getFechaNacimiento(), LocalDate.now()).getYears();
        }

        boolean tieneHC = p.getHistoriasClinicas() != null && !p.getHistoriasClinicas().isEmpty();

        java.time.Instant ultimaAtencion = p.getHistoriasClinicas().stream()
                .filter(hc -> hc.getAtenciones() != null)
                .flatMap(hc -> hc.getAtenciones().stream())
                .map(a -> a.getFechaAtencion())
                .filter(f -> f != null)
                .max(java.time.Instant::compareTo)
                .orElse(null);

        boolean facturaPendiente = facturaRepository.existeFacturaPendienteByPacienteId(p.getId());

        return ConsultaMedicaDto.builder()
                .id(c.getId())
                .fechaHora(c.getFechaHora())
                .servicio(c.getServicio())
                .estado(c.getEstado())
                .notas(c.getNotas())
                .motivoCancelacion(c.getMotivoCancelacion())
                .pacienteId(p.getId())
                .pacienteNombreCompleto(nombreCompleto)
                .pacienteDocumento(p.getDocumento())
                .pacienteTipoDocumento(p.getTipoDocumento())
                .pacienteEdad(edad)
                .pacienteSexo(p.getSexo())
                .pacienteGrupoSanguineo(p.getGrupoSanguineo())
                .pacienteTelefono(p.getTelefono())
                .pacienteEps(epsNombre)
                .pacienteEpsCodigo(epsCodigo)
                .profesionalId(prof.getId())
                .profesionalNombre(nombreProf)
                .profesionalRol(prof.getRol())
                .tieneHistoriaClinica(tieneHC)
                .tieneFacturasPendientes(facturaPendiente)
                .ultimaAtencion(ultimaAtencion)
                .createdAt(c.getCreatedAt())
                .build();
    }
}
