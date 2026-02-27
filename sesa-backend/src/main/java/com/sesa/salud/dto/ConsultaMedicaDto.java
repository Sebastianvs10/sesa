/**
 * DTO enriquecido para el módulo Consulta Médica.
 * Incluye datos del paciente, alertas clínicas y del profesional.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultaMedicaDto {

    // ── Cita ──────────────────────────────────────────────────────────────
    private Long id;
    private LocalDateTime fechaHora;
    private String servicio;
    private String estado;
    private String notas;
    private String motivoCancelacion;

    // ── Paciente ──────────────────────────────────────────────────────────
    private Long pacienteId;
    private String pacienteNombreCompleto;
    private String pacienteDocumento;
    private String pacienteTipoDocumento;
    private Integer pacienteEdad;
    private String pacienteSexo;
    private String pacienteGrupoSanguineo;
    private String pacienteTelefono;
    private String pacienteEps;
    private String pacienteEpsCodigo;

    // ── Profesional ───────────────────────────────────────────────────────
    private Long profesionalId;
    private String profesionalNombre;
    private String profesionalRol;

    // ── Alertas / indicadores clínicos ───────────────────────────────────
    private Boolean tieneHistoriaClinica;
    private Boolean tieneFacturasPendientes;
    private Instant ultimaAtencion;

    private Instant createdAt;
}
