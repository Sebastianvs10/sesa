/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.ProgramacionMesDto;
import com.sesa.salud.dto.ResumenProfesionalDto;
import com.sesa.salud.dto.TurnoDto;
import com.sesa.salud.dto.TurnoRequestDto;
import com.sesa.salud.entity.enums.ServicioClinico;
import com.sesa.salud.entity.enums.TipoTurno;

import java.util.List;

/** Contrato del servicio de gestión de turnos para la Agenda IPS Nivel II. */
public interface TurnoService {

    // ── Programación mensual ─────────────────────────────────────

    /**
     * Devuelve o crea (en estado BORRADOR) la programación del mes indicado.
     *
     * @param anio       Año calendario (ej. 2025).
     * @param mes        Mes 1-based (1 = enero).
     * @param usuarioId  ID del usuario que consulta (para autoría).
     * @param usuarioNombre Nombre del usuario que consulta.
     */
    ProgramacionMesDto getOrCrearProgramacion(int anio, int mes, Long usuarioId, String usuarioNombre);

    /** Cambia el estado de la programación a EN_REVISION (solo Jefe de Enfermería). */
    ProgramacionMesDto enviarARevision(int anio, int mes, Long usuarioId);

    /** Aprueba la programación (solo Coordinador Médico). */
    ProgramacionMesDto aprobar(int anio, int mes, Long usuarioId, String usuarioNombre);

    /** Cierra el mes: bloquea cualquier modificación futura. */
    ProgramacionMesDto cerrar(int anio, int mes, Long usuarioId);

    // ── Turnos ──────────────────────────────────────────────────

    /**
     * Lista todos los turnos del mes con filtros opcionales.
     *
     * @param servicio   {@code null} para no filtrar por servicio.
     * @param tipoTurno  {@code null} para no filtrar por tipo.
     * @param personalId {@code null} para no filtrar por profesional.
     */
    List<TurnoDto> listar(int anio, int mes, ServicioClinico servicio, TipoTurno tipoTurno, Long personalId);

    /** Crea un nuevo turno validando las reglas colombianas del CST. */
    TurnoDto crear(TurnoRequestDto request, Long usuarioId);

    /** Actualiza un turno existente revalidando las reglas. */
    TurnoDto actualizar(Long turnoId, TurnoRequestDto request, Long usuarioId);

    /** Mueve un turno a otra fecha (drag & drop en el frontend). */
    TurnoDto moverFecha(Long turnoId, java.time.LocalDate nuevaFecha, Long usuarioId);

    /** Elimina un turno. No permitido si el mes está CERRADO. */
    void eliminar(Long turnoId);

    // ── Resúmenes ───────────────────────────────────────────────

    /** Devuelve el resumen de horas de todos los profesionales visibles en el mes. */
    List<ResumenProfesionalDto> resumenMes(int anio, int mes);

    /** Devuelve el resumen de horas de un profesional específico. */
    ResumenProfesionalDto resumenProfesional(Long personalId, int anio, int mes);
}
