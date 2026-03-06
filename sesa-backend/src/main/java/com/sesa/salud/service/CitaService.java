/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.CitaDto;
import com.sesa.salud.dto.CitaRequestDto;
import com.sesa.salud.dto.ConsultaMedicaDto;
import com.sesa.salud.dto.ConsultasStatsDto;
import com.sesa.salud.entity.Personal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface CitaService {

    Page<CitaDto> findAll(Pageable pageable);

    List<CitaDto> findByFecha(LocalDate fecha);

    /** Citas de una fecha opcionalmente filtradas por profesional (para slots por especialista). */
    List<CitaDto> findByFechaAndProfesionalId(LocalDate fecha, Long profesionalId);

    List<CitaDto> findByPacienteId(Long pacienteId, Pageable pageable);

    CitaDto findById(Long id);

    CitaDto create(CitaRequestDto dto);

    CitaDto update(Long id, CitaRequestDto dto);

    void deleteById(Long id);

    // ── Módulo Consulta Médica ─────────────────────────────────────────────

    /** Citas enriquecidas del día para un profesional específico. */
    List<ConsultaMedicaDto> findConsultasMedicas(Long profesionalId, LocalDate fecha);

    /** Citas enriquecidas del día para todos los profesionales (vista Admin). */
    List<ConsultaMedicaDto> findConsultasMedicasTodas(LocalDate fecha);

    /** Estadísticas del día para un profesional (o de todos si profesionalId == null). */
    ConsultasStatsDto getStatsDelDia(Long profesionalId, LocalDate fecha);

    /** Cancela la cita con un motivo. */
    CitaDto cancelarCita(Long id, String motivo);

    /** Cambia el estado de la cita. */
    CitaDto cambiarEstado(Long id, String nuevoEstado);

    /** Lista de profesionales con roles MEDICO o JEFE_ENFERMERIA para filtrado admin. */
    List<Personal> findProfesionalesMedicos();
}
