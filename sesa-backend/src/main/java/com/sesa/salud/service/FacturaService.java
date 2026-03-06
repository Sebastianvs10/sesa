/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.FacturaDto;
import com.sesa.salud.dto.FacturaRequestDto;
import com.sesa.salud.dto.ResumenFacturacionDto;
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
}
