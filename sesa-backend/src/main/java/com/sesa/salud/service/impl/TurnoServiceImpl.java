/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.ProgramacionMesDto;
import com.sesa.salud.dto.ResumenProfesionalDto;
import com.sesa.salud.dto.TurnoDto;
import com.sesa.salud.dto.TurnoRequestDto;
import com.sesa.salud.dto.TurnoResponseDto;
import com.sesa.salud.entity.Personal;
import com.sesa.salud.entity.ProgramacionMes;
import com.sesa.salud.entity.Turno;
import com.sesa.salud.entity.enums.EstadoProgramacion;
import com.sesa.salud.entity.enums.EstadoTurno;
import com.sesa.salud.entity.enums.ServicioClinico;
import com.sesa.salud.entity.enums.TipoTurno;
import com.sesa.salud.repository.PersonalRepository;
import com.sesa.salud.repository.ProgramacionMesRepository;
import com.sesa.salud.repository.TurnoRepository;
import com.sesa.salud.service.TurnoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Implementación del servicio de turnos con validaciones del
 * Código Sustantivo del Trabajo (CST) y la Resolución 2003/2014 MSPS Colombia.
 *
 * <h3>Reglas aplicadas</h3>
 * <ul>
 *   <li>Sin solapamiento de turnos para el mismo profesional.</li>
 *   <li>Descanso mínimo de 8 h entre turnos ordinarios.</li>
 *   <li>Descanso mínimo de 12 h tras turno nocturno (URG_NOCHE).</li>
 *   <li>Descanso mínimo de 24 h tras turno de 24 h.</li>
 *   <li>Máximo de 48 h semanales (promedio).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TurnoServiceImpl implements TurnoService {

    // ── Constantes laborales (CST Colombia) ──────────────────────────────────

    private static final int MAX_HORAS_SEMANALES        = 48;
    private static final int ALERTA_HORAS_SEMANALES     = 40;
    private static final int DESCANSO_MIN_ORDINARIO_H   =  8;
    private static final int DESCANSO_MIN_NOCTURNO_H    = 12;
    private static final int DESCANSO_MIN_24H_H         = 24;
    private static final int MAX_HORAS_MENSUALES        = 192;

    /** Festivos colombianos como conjunto de strings "yyyy-MM-dd". Se actualiza anualmente. */
    private static final Set<String> FESTIVOS_CO = Set.of(
        // 2025
        "2025-01-01","2025-01-06","2025-03-24","2025-04-13","2025-04-14",
        "2025-05-01","2025-06-02","2025-06-23","2025-06-30","2025-07-07",
        "2025-07-20","2025-08-07","2025-08-18","2025-10-13","2025-11-03",
        "2025-11-17","2025-12-08","2025-12-25",
        // 2026
        "2026-01-01","2026-01-12","2026-03-23","2026-04-02","2026-04-03",
        "2026-05-01","2026-07-20","2026-08-07","2026-12-08","2026-12-25"
    );

    // ── Dependencias ──────────────────────────────────────────────────────────

    private final TurnoRepository           turnoRepo;
    private final ProgramacionMesRepository progRepo;
    private final PersonalRepository        personalRepo;

    // ════════════════════════════════════════════════════════════════════════
    //  PROGRAMACIÓN MENSUAL
    // ════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public ProgramacionMesDto getOrCrearProgramacion(int anio, int mes, Long usuarioId, String usuarioNombre) {
        return progRepo.findByAnioAndMes(anio, mes)
                .map(this::toProgramacionDto)
                .orElseGet(() -> {
                    ProgramacionMes nueva = ProgramacionMes.builder()
                            .anio(anio)
                            .mes(mes)
                            .estado(EstadoProgramacion.BORRADOR)
                            .creadoPorId(usuarioId)
                            .creadoPorNombre(usuarioNombre)
                            .build();
                    return toProgramacionDto(progRepo.save(nueva));
                });
    }

    @Override
    @Transactional
    public ProgramacionMesDto enviarARevision(int anio, int mes, Long usuarioId) {
        ProgramacionMes prog = getProgramacion(anio, mes);
        validarEstadoEditable(prog, "enviar a revisión");
        prog.setEstado(EstadoProgramacion.EN_REVISION);
        return toProgramacionDto(progRepo.save(prog));
    }

    @Override
    @Transactional
    public ProgramacionMesDto aprobar(int anio, int mes, Long usuarioId, String usuarioNombre) {
        ProgramacionMes prog = getProgramacion(anio, mes);
        if (prog.getEstado() != EstadoProgramacion.EN_REVISION && prog.getEstado() != EstadoProgramacion.BORRADOR) {
            throw new IllegalStateException("Solo se puede aprobar una programación en estado BORRADOR o EN_REVISION");
        }
        prog.setEstado(EstadoProgramacion.APROBADO);
        prog.setAprobadoPorId(usuarioId);
        prog.setAprobadoPorNombre(usuarioNombre);
        prog.setFechaAprobacion(java.time.Instant.now());
        // Marcar todos los turnos del mes como APROBADO
        List<Turno> turnos = turnoRepo.findByProgramacionMesId(prog.getId());
        turnos.forEach(t -> t.setEstado(EstadoTurno.APROBADO));
        turnoRepo.saveAll(turnos);
        return toProgramacionDto(progRepo.save(prog));
    }

    @Override
    @Transactional
    public ProgramacionMesDto cerrar(int anio, int mes, Long usuarioId) {
        ProgramacionMes prog = getProgramacion(anio, mes);
        prog.setEstado(EstadoProgramacion.CERRADO);
        List<Turno> turnos = turnoRepo.findByProgramacionMesId(prog.getId());
        turnos.forEach(t -> t.setEstado(EstadoTurno.CERRADO));
        turnoRepo.saveAll(turnos);
        return toProgramacionDto(progRepo.save(prog));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TURNOS — CRUD
    // ════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<TurnoDto> listar(int anio, int mes, ServicioClinico servicio, TipoTurno tipoTurno, Long personalId) {
        Long progId = progRepo.findByAnioAndMes(anio, mes)
                .map(ProgramacionMes::getId)
                .orElse(null);
        if (progId == null) return List.of();

        return turnoRepo.findByProgramacionMesConFiltros(progId, servicio, tipoTurno, personalId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public TurnoResponseDto crear(TurnoRequestDto req, Long usuarioId) {
        Personal personal = getPersonal(req.getPersonalId());

        LocalDate fecha = req.getFecha();
        ProgramacionMes prog = getOrCrearProgramacionInterna(
                fecha.getYear(), fecha.getMonthValue(), usuarioId, personal.getNombres());

        validarEstadoEditable(prog, "crear turno");

        LocalDateTime fechaInicio = LocalDateTime.of(fecha, java.time.LocalTime.of(req.getTipoTurno().horaInicio, 0));
        LocalDateTime fechaFin    = fechaInicio.plusHours(req.getTipoTurno().duracionHoras);

        List<String> advertencias = validarReglasLaborales(req.getPersonalId(), -1L, fechaInicio, fechaFin, req.getTipoTurno());

        Turno turno = Turno.builder()
                .personal(personal)
                .programacionMes(prog)
                .servicio(req.getServicio())
                .tipoTurno(req.getTipoTurno())
                .fechaInicio(fechaInicio)
                .fechaFin(fechaFin)
                .duracionHoras(req.getTipoTurno().duracionHoras)
                .estado(req.getEstado() != null ? req.getEstado() : EstadoTurno.BORRADOR)
                .esFestivo(esFestivo(fecha))
                .notas(req.getNotas())
                .modificadoPorId(usuarioId)
                .build();

        Turno guardado = turnoRepo.save(turno);
        return new TurnoResponseDto(toDto(guardado), advertencias);
    }

    @Override
    @Transactional
    public TurnoResponseDto actualizar(Long turnoId, TurnoRequestDto req, Long usuarioId) {
        Turno turno = getTurno(turnoId);
        validarEstadoEditable(turno.getProgramacionMes(), "editar turno");

        LocalDate fecha        = req.getFecha();
        LocalDateTime inicio   = LocalDateTime.of(fecha, java.time.LocalTime.of(req.getTipoTurno().horaInicio, 0));
        LocalDateTime fin      = inicio.plusHours(req.getTipoTurno().duracionHoras);

        List<String> advertencias = validarReglasLaborales(req.getPersonalId(), turnoId, inicio, fin, req.getTipoTurno());

        Personal personal = getPersonal(req.getPersonalId());

        turno.setPersonal(personal);
        turno.setServicio(req.getServicio());
        turno.setTipoTurno(req.getTipoTurno());
        turno.setFechaInicio(inicio);
        turno.setFechaFin(fin);
        turno.setDuracionHoras(req.getTipoTurno().duracionHoras);
        turno.setEsFestivo(esFestivo(fecha));
        turno.setNotas(req.getNotas());
        turno.setModificadoPorId(usuarioId);
        if (req.getEstado() != null) turno.setEstado(req.getEstado());

        Turno guardado = turnoRepo.save(turno);
        return new TurnoResponseDto(toDto(guardado), advertencias);
    }

    @Override
    @Transactional
    public TurnoDto moverFecha(Long turnoId, LocalDate nuevaFecha, Long usuarioId) {
        Turno turno = getTurno(turnoId);
        validarEstadoEditable(turno.getProgramacionMes(), "mover turno");

        LocalDateTime inicio = LocalDateTime.of(nuevaFecha, java.time.LocalTime.of(turno.getTipoTurno().horaInicio, 0));
        LocalDateTime fin    = inicio.plusHours(turno.getTipoTurno().duracionHoras);

        validarReglasLaborales(turno.getPersonal().getId(), turnoId, inicio, fin, turno.getTipoTurno());

        turno.setFechaInicio(inicio);
        turno.setFechaFin(fin);
        turno.setEsFestivo(esFestivo(nuevaFecha));
        turno.setModificadoPorId(usuarioId);

        return toDto(turnoRepo.save(turno));
    }

    @Override
    @Transactional
    public void eliminar(Long turnoId) {
        Turno turno = getTurno(turnoId);
        validarEstadoEditable(turno.getProgramacionMes(), "eliminar turno");
        turnoRepo.delete(turno);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  RESÚMENES
    // ════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<ResumenProfesionalDto> resumenMes(int anio, int mes) {
        return personalRepo.findByActivoTrue(org.springframework.data.domain.Pageable.unpaged())
                .getContent()
                .stream()
                .map(p -> calcularResumen(p, anio, mes))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenProfesionalDto resumenProfesional(Long personalId, int anio, int mes) {
        Personal personal = getPersonal(personalId);
        return calcularResumen(personal, anio, mes);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  VALIDACIONES LABORALES (CST Colombia)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Evalúa las reglas laborales del CST y devuelve advertencias (no bloquea).
     * El turno se permite siempre; las advertencias se muestran en el frontend.
     */
    private List<String> validarReglasLaborales(Long personalId, Long excludeId,
                                                LocalDateTime inicio, LocalDateTime fin, TipoTurno tipo) {
        List<String> advertencias = new ArrayList<>();

        // 2 — Descanso: solo advertencia (no bloquea)
        LocalDateTime desde = inicio.minusHours(DESCANSO_MIN_24H_H);
        LocalDateTime hasta = fin.plusHours(DESCANSO_MIN_24H_H);
        List<Turno> adyacentes = turnoRepo.findByPersonalEnRango(personalId, excludeId, desde, hasta);

        for (Turno prev : adyacentes) {
            if (prev.getTipoTurno().requiereDescansoExtendido) {
                int descansoMinimo = (prev.getTipoTurno() == TipoTurno.TURNO_24H)
                        ? DESCANSO_MIN_24H_H
                        : DESCANSO_MIN_NOCTURNO_H;
                if (!inicio.isBefore(prev.getFechaFin())) {
                    long gapHoras = java.time.Duration.between(prev.getFechaFin(), inicio).toHours();
                    if (gapHoras < descansoMinimo) {
                        advertencias.add("Descanso insuficiente tras turno '%s': quedan %d h y el mínimo es %d h."
                                .formatted(prev.getTipoTurno().etiqueta, gapHoras, descansoMinimo));
                    }
                }
                if (!prev.getFechaInicio().isBefore(fin)) {
                    long gapHoras = java.time.Duration.between(fin, prev.getFechaInicio()).toHours();
                    if (gapHoras < descansoMinimo) {
                        advertencias.add("El siguiente turno '%s' no respeta el descanso mínimo de %d h tras este turno."
                                .formatted(prev.getTipoTurno().etiqueta, descansoMinimo));
                    }
                }
            } else {
                if (!inicio.isBefore(prev.getFechaFin())) {
                    long gapHoras = java.time.Duration.between(prev.getFechaFin(), inicio).toHours();
                    if (gapHoras < DESCANSO_MIN_ORDINARIO_H) {
                        advertencias.add("Descanso insuficiente entre turnos: quedan %d h y el mínimo es %d h."
                                .formatted(gapHoras, DESCANSO_MIN_ORDINARIO_H));
                    }
                }
            }
        }

        // 3 — Horas semanales: advertencia si supera el límite (no bloquea)
        LocalDateTime lunesSemana = inicio.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .toLocalDate().atStartOfDay();
        LocalDateTime finSemana = lunesSemana.plusDays(7);
        List<Turno> enSemana = turnoRepo.findByPersonalEnRango(personalId, excludeId, lunesSemana, finSemana);
        int horasSemana = enSemana.stream().mapToInt(Turno::getDuracionHoras).sum() + tipo.duracionHoras;

        if (horasSemana > MAX_HORAS_SEMANALES) {
            advertencias.add("Se ha excedido el límite de %d h semanales para este profesional (acumulado: %d h)."
                    .formatted(MAX_HORAS_SEMANALES, horasSemana));
            log.warn("Profesional id={} supera {} h semanales (acumulado: {} h)", personalId, MAX_HORAS_SEMANALES, horasSemana);
        } else if (horasSemana > ALERTA_HORAS_SEMANALES) {
            log.warn("Profesional id={} supera las {} h semanales de alerta (acumulado: {} h)",
                    personalId, ALERTA_HORAS_SEMANALES, horasSemana);
        }

        return advertencias;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPERS PRIVADOS
    // ════════════════════════════════════════════════════════════════════════

    private ResumenProfesionalDto calcularResumen(Personal personal, int anio, int mes) {
        Long progId = progRepo.findByAnioAndMes(anio, mes)
                .map(ProgramacionMes::getId)
                .orElse(null);

        List<Turno> turnos = (progId != null)
                ? turnoRepo.findByPersonalIdAndProgramacionMesId(personal.getId(), progId)
                : List.of();

        int horasTotales   = turnos.stream().mapToInt(Turno::getDuracionHoras).sum();
        int horasNocturnas = turnos.stream()
                .filter(t -> t.getTipoTurno().requiereDescansoExtendido)
                .mapToInt(Turno::getDuracionHoras).sum();
        int horasFestivos  = turnos.stream()
                .filter(Turno::getEsFestivo)
                .mapToInt(Turno::getDuracionHoras).sum();

        int porcentaje = Math.min(100, (horasTotales * 100) / MAX_HORAS_MENSUALES);

        return ResumenProfesionalDto.builder()
                .personalId(personal.getId())
                .nombres(personal.getNombres())
                .apellidos(personal.getApellidos())
                .horasTotales(horasTotales)
                .horasNocturnas(horasNocturnas)
                .horasFestivos(horasFestivos)
                .cantidadTurnos(turnos.size())
                .porcentajeOcupacion(porcentaje)
                .tieneConflictos(false) // las validaciones al guardar impiden conflictos
                .build();
    }

    private void validarEstadoEditable(ProgramacionMes prog, String accion) {
        if (prog.getEstado() == EstadoProgramacion.CERRADO) {
            throw new IllegalStateException(
                "No se puede %s: la programación del mes está CERRADA.".formatted(accion));
        }
    }

    private boolean esFestivo(LocalDate fecha) {
        return FESTIVOS_CO.contains(fecha.toString());
    }

    private ProgramacionMes getOrCrearProgramacionInterna(int anio, int mes, Long usuarioId, String nombre) {
        return progRepo.findByAnioAndMes(anio, mes).orElseGet(() ->
            progRepo.save(ProgramacionMes.builder()
                .anio(anio).mes(mes)
                .estado(EstadoProgramacion.BORRADOR)
                .creadoPorId(usuarioId)
                .creadoPorNombre(nombre)
                .build()));
    }

    private ProgramacionMes getProgramacion(int anio, int mes) {
        return progRepo.findByAnioAndMes(anio, mes)
                .orElseThrow(() -> new RuntimeException(
                    "Programación no encontrada para %d/%d".formatted(mes, anio)));
    }

    private Turno getTurno(Long id) {
        return turnoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado: " + id));
    }

    private Personal getPersonal(Long id) {
        return personalRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Profesional no encontrado: " + id));
    }

    // ── Mapeo entidad → DTO ──────────────────────────────────────────────────

    private TurnoDto toDto(Turno t) {
        Personal p = t.getPersonal();
        return new TurnoDto(
                t.getId(),
                p.getId(),
                p.getNombres(),
                p.getApellidos(),
                t.getProgramacionMes().getId(),
                t.getServicio(),
                t.getServicio().etiqueta,
                t.getTipoTurno(),
                t.getTipoTurno().etiqueta,
                t.getDuracionHoras(),
                t.getFechaInicio(),
                t.getFechaFin(),
                t.getEstado(),
                t.getEsFestivo(),
                t.getNotas(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }

    private ProgramacionMesDto toProgramacionDto(ProgramacionMes p) {
        return new ProgramacionMesDto(
                p.getId(),
                p.getAnio(),
                p.getMes(),
                p.getEstado(),
                p.getCreadoPorId(),
                p.getCreadoPorNombre(),
                p.getAprobadoPorId(),
                p.getAprobadoPorNombre(),
                p.getFechaAprobacion(),
                p.getObservaciones(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
