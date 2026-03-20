/**
 * Servicio del módulo de Farmacia — inventario, dispensación y órdenes médicas.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface FarmaciaService {
    Page<FarmaciaMedicamentoDto> listMedicamentos(String q, boolean soloStock, Pageable pageable);

    FarmaciaIndicadoresDto indicadoresInventario();

    Optional<FarmaciaMedicamentoDto> findMedicamentoPorCodigoBarras(String codigo);

    FarmaciaMedicamentoDto createMedicamento(FarmaciaMedicamentoRequestDto dto);
    FarmaciaMedicamentoDto updateMedicamento(Long id, FarmaciaMedicamentoRequestDto dto);
    void deleteMedicamento(Long id);

    List<FarmaciaDispensacionDto> listDispensacionesByPaciente(Long pacienteId, Pageable pageable);
    FarmaciaDispensacionDto dispensar(FarmaciaDispensacionRequestDto dto);

    Page<OrdenFarmaciaPendienteDto> listOrdenesPendientes(String q, Pageable pageable);
    List<FarmaciaDispensacionDto> dispensarOrden(DispensarOrdenRequestDto dto);
}
