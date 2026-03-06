/**
 * Servicio del módulo de Farmacia — inventario, dispensación y órdenes médicas.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FarmaciaService {
    List<FarmaciaMedicamentoDto> listMedicamentos(Pageable pageable);
    FarmaciaMedicamentoDto createMedicamento(FarmaciaMedicamentoRequestDto dto);
    FarmaciaMedicamentoDto updateMedicamento(Long id, FarmaciaMedicamentoRequestDto dto);
    void deleteMedicamento(Long id);

    List<FarmaciaDispensacionDto> listDispensacionesByPaciente(Long pacienteId, Pageable pageable);
    FarmaciaDispensacionDto dispensar(FarmaciaDispensacionRequestDto dto);

    /** Órdenes clínicas tipo MEDICAMENTO pendientes o parciales de dispensar (para regente). */
    List<OrdenFarmaciaPendienteDto> listOrdenesPendientes(Pageable pageable);
    /** Dispensar una orden médica: varias líneas (medicamento, lote, cantidad); descuenta stock y vincula a la orden. */
    List<FarmaciaDispensacionDto> dispensarOrden(DispensarOrdenRequestDto dto);
}
