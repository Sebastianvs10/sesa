/**
 * Interfaz del servicio Odontología.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.odontologia.*;
import com.sesa.salud.entity.ProcedimientoCatalogo;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface OdontologiaService {

    // Consultas
    List<ConsultaOdontologicaDto> getConsultasByPaciente(Long pacienteId, Pageable pageable);
    ConsultaOdontologicaDto getConsultaById(Long id);
    ConsultaOdontologicaDto crearConsulta(ConsultaOdontologicaDto dto);
    ConsultaOdontologicaDto actualizarConsulta(Long id, ConsultaOdontologicaDto dto);

    // Odontograma
    List<OdontogramaEstadoDto> getOdontograma(Long pacienteId);
    List<OdontogramaEstadoDto> getOdontogramaByConsulta(Long consultaId);
    OdontogramaEstadoDto guardarEstadoPieza(OdontogramaEstadoDto dto);
    List<OdontogramaEstadoDto> guardarOdontogramaBatch(List<OdontogramaEstadoDto> cambios);

    // Catálogo procedimientos
    List<ProcedimientoCatalogo> getCatalogoProcedimientos();
    ProcedimientoCatalogo crearProcedimientoCatalogo(ProcedimientoCatalogo dto);
    ProcedimientoCatalogo actualizarProcedimientoCatalogo(Long id, ProcedimientoCatalogo dto);

    // Planes de tratamiento
    List<PlanTratamientoDto> getPlanesByPaciente(Long pacienteId);
    PlanTratamientoDto getPlanById(Long id);
    PlanTratamientoDto crearPlan(PlanTratamientoDto dto);
    PlanTratamientoDto actualizarPlan(Long id, PlanTratamientoDto dto);
    PlanTratamientoDto cambiarEstadoPlan(Long id, String estado);
    PlanTratamientoDto registrarAbono(Long id, BigDecimal monto);

    // Imágenes
    List<ImagenClinicaDto> getImagenesByPaciente(Long pacienteId);
    ImagenClinicaDto subirImagen(ImagenClinicaDto dto);
    void eliminarImagen(Long id);

    // Evoluciones
    List<EvolucionOdontologicaDto> getEvolucionesByPaciente(Long pacienteId);
    EvolucionOdontologicaDto registrarEvolucion(EvolucionOdontologicaDto dto);

    // Stats
    Map<String, Object> getStatsDelDia(Long profesionalId);
}
