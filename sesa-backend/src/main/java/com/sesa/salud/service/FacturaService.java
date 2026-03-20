/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.AlertasFacturacionDto;
import com.sesa.salud.dto.BandejaFacturadorDto;
import com.sesa.salud.dto.ChecklistRadicacionDto;
import com.sesa.salud.dto.FacturaDto;
import com.sesa.salud.dto.FacturaRequestDto;
import com.sesa.salud.dto.ResumenFacturacionDto;
import com.sesa.salud.dto.FacturaDetalleCompletoDto;
import com.sesa.salud.dto.FacturaTimelineEventDto;
import com.sesa.salud.dto.FacturaLoteResultDto;
import com.sesa.salud.dto.OrdenPendienteFacturaDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface FacturaService {
    List<FacturaDto> findByPacienteId(Long pacienteId, Pageable pageable);
    Page<FacturaDto> findAllFiltered(String estado, Instant desde, Instant hasta, Long pacienteId, Pageable pageable);
    FacturaDto findById(Long id);
    FacturaDto create(FacturaRequestDto dto);
    FacturaDto update(Long id, FacturaRequestDto dto);
    FacturaDto cambiarEstado(Long id, String nuevoEstado);
    void deleteById(Long id);
    String exportRipsCsv(Instant desde, Instant hasta);
    Map<String, String> exportRipsEstructurado(Instant desde, Instant hasta);
    ResumenFacturacionDto resumen();

    /** Emite una factura como electrónica DIAN (si la empresa tiene la opción activa). */
    FacturaDto emitirElectronica(Long id);

    /** Detalle con trazabilidad a orden médica y consulta (historia clínica). */
    FacturaDetalleCompletoDto findDetalleCompleto(Long id);

    /** Línea de tiempo de la factura: creada → emitida FEV → radicada → glosa → pagada/rechazada/anulada. */
    List<FacturaTimelineEventDto> getTimeline(Long facturaId);

    /** Órdenes clínicas (medicamentos, laboratorio, procedimientos) sin factura asociada — pendientes de facturar. */
    Page<OrdenPendienteFacturaDto> getOrdenesPendientesDeFacturar(Pageable pageable);

    /** Alertas para dashboard predictivo (facturas por vencer/vencidas radicación, glosas pendientes). */
    AlertasFacturacionDto getAlertas();

    /** Checklist pre-radicación: validaciones antes de radicar la factura ante EPS. */
    ChecklistRadicacionDto getChecklistRadicacion(Long facturaId);

    /** Bandeja del facturador: tareas (radicar, responder glosas). */
    BandejaFacturadorDto getBandejaFacturador();

    /** Facturación por lote: agrupa órdenes por paciente y crea una factura por paciente con ítems = órdenes. */
    FacturaLoteResultDto createFromLote(List<Long> ordenIds);

    /** Exporta el libro de facturación (CSV) para un período y estado opcional. */
    String exportLibroCsv(Instant desde, Instant hasta, String estado);
}
