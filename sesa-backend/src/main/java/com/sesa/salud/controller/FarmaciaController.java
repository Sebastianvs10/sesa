/**
 * Controlador del módulo de Farmacia — medicamentos, inventario, órdenes médicas y dispensación.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.dto.*;
import com.sesa.salud.service.FarmaciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/farmacia")
@RequiredArgsConstructor
public class FarmaciaController {

    private final FarmaciaService farmaciaService;

    @GetMapping("/medicamentos")
    @PreAuthorize("hasAnyRole('ADMIN','REGENTE_FARMACIA','MEDICO','SUPERADMINISTRADOR','FACTURACION','ENFERMERO','RECEPCIONISTA')")
    public List<FarmaciaMedicamentoDto> listMedicamentos(@PageableDefault(size = 50) Pageable pageable) {
        return farmaciaService.listMedicamentos(pageable);
    }

    @PostMapping("/medicamentos")
    @PreAuthorize("hasAnyRole('ADMIN','REGENTE_FARMACIA','SUPERADMINISTRADOR')")
    public ResponseEntity<FarmaciaMedicamentoDto> createMedicamento(@Valid @RequestBody FarmaciaMedicamentoRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(farmaciaService.createMedicamento(dto));
    }

    @PutMapping("/medicamentos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','REGENTE_FARMACIA','SUPERADMINISTRADOR')")
    public ResponseEntity<FarmaciaMedicamentoDto> updateMedicamento(@PathVariable("id") Long id, @Valid @RequestBody FarmaciaMedicamentoRequestDto dto) {
        return ResponseEntity.ok(farmaciaService.updateMedicamento(id, dto));
    }

    @DeleteMapping("/medicamentos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','REGENTE_FARMACIA','SUPERADMINISTRADOR')")
    public ResponseEntity<Void> deleteMedicamento(@PathVariable("id") Long id) {
        farmaciaService.deleteMedicamento(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dispensaciones/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ADMIN','REGENTE_FARMACIA','MEDICO','SUPERADMINISTRADOR','FACTURACION','ENFERMERO')")
    public List<FarmaciaDispensacionDto> listDispensaciones(@PathVariable("pacienteId") Long pacienteId,
                                                            @PageableDefault(size = 30) Pageable pageable) {
        return farmaciaService.listDispensacionesByPaciente(pacienteId, pageable);
    }

    @PostMapping("/dispensar")
    @PreAuthorize("hasAnyRole('ADMIN','REGENTE_FARMACIA','SUPERADMINISTRADOR')")
    public ResponseEntity<FarmaciaDispensacionDto> dispensar(@Valid @RequestBody FarmaciaDispensacionRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(farmaciaService.dispensar(dto));
    }

    /** Órdenes clínicas tipo MEDICAMENTO pendientes o parciales de dispensar (desde Historia Clínica). */
    @GetMapping("/ordenes-pendientes")
    @PreAuthorize("hasAnyRole('ADMIN','REGENTE_FARMACIA','MEDICO','SUPERADMINISTRADOR','FACTURACION','ENFERMERO','RECEPCIONISTA')")
    public List<OrdenFarmaciaPendienteDto> listOrdenesPendientes(@PageableDefault(size = 50) Pageable pageable) {
        return farmaciaService.listOrdenesPendientes(pageable);
    }

    /** Dispensar una orden médica por ID: varias líneas (medicamento, cantidad); descuenta stock y vincula a la orden. */
    @PostMapping("/dispensar-orden")
    @PreAuthorize("hasAnyRole('ADMIN','REGENTE_FARMACIA','SUPERADMINISTRADOR')")
    public ResponseEntity<List<FarmaciaDispensacionDto>> dispensarOrden(@Valid @RequestBody DispensarOrdenRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(farmaciaService.dispensarOrden(dto));
    }
}
