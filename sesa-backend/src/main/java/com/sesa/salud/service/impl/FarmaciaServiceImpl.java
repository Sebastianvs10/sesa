/**
 * Implementación del módulo de Farmacia — inventario, dispensación y órdenes médicas (HC).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.*;
import com.sesa.salud.entity.FarmaciaDispensacion;
import com.sesa.salud.entity.FarmaciaMedicamento;
import com.sesa.salud.entity.OrdenClinica;
import com.sesa.salud.entity.OrdenClinicaItem;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.repository.FarmaciaDispensacionRepository;
import com.sesa.salud.repository.FarmaciaMedicamentoRepository;
import com.sesa.salud.repository.HistoriaClinicaRepository;
import com.sesa.salud.repository.OrdenClinicaRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.service.FarmaciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FarmaciaServiceImpl implements FarmaciaService {

    private static final String TIPO_MEDICAMENTO = "MEDICAMENTO";

    private final FarmaciaMedicamentoRepository medicamentoRepository;
    private final FarmaciaDispensacionRepository dispensacionRepository;
    private final PacienteRepository pacienteRepository;
    private final OrdenClinicaRepository ordenClinicaRepository;
    private final HistoriaClinicaRepository historiaClinicaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<FarmaciaMedicamentoDto> listMedicamentos(String q, boolean soloStock, Pageable pageable) {
        String param = (q == null || q.isBlank()) ? "" : q.trim();
        return medicamentoRepository.searchPaged(param, soloStock, pageable).map(this::toMedicamentoDto);
    }

    @Override
    @Transactional(readOnly = true)
    public FarmaciaIndicadoresDto indicadoresInventario() {
        LocalDate hoy = LocalDate.now();
        LocalDate en30 = hoy.plusDays(30);
        return FarmaciaIndicadoresDto.builder()
                .totalSkusActivos(medicamentoRepository.countActivos())
                .stockBajo(medicamentoRepository.countStockBajo())
                .proximosAVencer30Dias(medicamentoRepository.countProximosAVencer(hoy, en30))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FarmaciaMedicamentoDto> findMedicamentoPorCodigoBarras(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        return medicamentoRepository.findFirstByCodigoBarrasIgnoreCaseAndActivoTrue(codigo.trim())
                .map(this::toMedicamentoDto);
    }

    @Override
    @Transactional
    public FarmaciaMedicamentoDto createMedicamento(FarmaciaMedicamentoRequestDto dto) {
        FarmaciaMedicamento m = FarmaciaMedicamento.builder()
                .nombre(dto.getNombre())
                .lote(dto.getLote())
                .codigoBarras(dto.getCodigoBarras() != null ? dto.getCodigoBarras().trim() : null)
                .fechaVencimiento(dto.getFechaVencimiento())
                .cantidad(dto.getCantidad() != null ? dto.getCantidad() : 0)
                .precio(dto.getPrecio())
                .stockMinimo(dto.getStockMinimo() != null ? dto.getStockMinimo() : 0)
                .activo(dto.getActivo() != null ? dto.getActivo() : true)
                .build();
        return toMedicamentoDto(medicamentoRepository.save(m));
    }

    @Override
    @Transactional
    public FarmaciaMedicamentoDto updateMedicamento(Long id, FarmaciaMedicamentoRequestDto dto) {
        FarmaciaMedicamento m = medicamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medicamento no encontrado: " + id));
        if (dto.getNombre() != null) m.setNombre(dto.getNombre());
        if (dto.getLote() != null) m.setLote(dto.getLote());
        if (dto.getFechaVencimiento() != null) m.setFechaVencimiento(dto.getFechaVencimiento());
        if (dto.getCantidad() != null) m.setCantidad(dto.getCantidad());
        if (dto.getPrecio() != null) m.setPrecio(dto.getPrecio());
        if (dto.getStockMinimo() != null) m.setStockMinimo(dto.getStockMinimo());
        if (dto.getActivo() != null) m.setActivo(dto.getActivo());
        return toMedicamentoDto(medicamentoRepository.save(m));
    }

    @Override
    @Transactional
    public void deleteMedicamento(Long id) {
        if (!medicamentoRepository.existsById(id)) {
            throw new RuntimeException("Medicamento no encontrado: " + id);
        }
        medicamentoRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmaciaDispensacionDto> listDispensacionesByPaciente(Long pacienteId, Pageable pageable) {
        return dispensacionRepository.findByPaciente_IdOrderByFechaDispensacionDesc(pacienteId, pageable)
                .stream().map(this::toDispensacionDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FarmaciaDispensacionDto dispensar(FarmaciaDispensacionRequestDto dto) {
        FarmaciaMedicamento med = medicamentoRepository.findById(dto.getMedicamentoId())
                .orElseThrow(() -> new RuntimeException("Medicamento no encontrado"));
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));
        if (dto.getCantidad() <= 0) throw new RuntimeException("Cantidad inválida");
        if (med.getCantidad() < dto.getCantidad()) throw new RuntimeException("Stock insuficiente");

        med.setCantidad(med.getCantidad() - dto.getCantidad());
        medicamentoRepository.save(med);

        FarmaciaDispensacion d = FarmaciaDispensacion.builder()
                .medicamento(med)
                .paciente(paciente)
                .cantidad(dto.getCantidad())
                .entregadoPor(dto.getEntregadoPor())
                .build();
        return toDispensacionDto(dispensacionRepository.save(d));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrdenFarmaciaPendienteDto> listOrdenesPendientes(String q, Pageable pageable) {
        List<String> estados = List.of("PENDIENTE", "PARCIAL");
        String trimmed = q != null ? q.trim() : "";
        String search = trimmed.isEmpty() ? null : trimmed;
        Page<OrdenClinica> page = ordenClinicaRepository.findOrdenesFarmaciaPendientesPage(estados, search, pageable);
        return page.map(this::toOrdenFarmaciaPendienteDto);
    }

    @Override
    @Transactional
    public List<FarmaciaDispensacionDto> dispensarOrden(DispensarOrdenRequestDto dto) {
        if (dto.getLineas() == null || dto.getLineas().isEmpty()) {
            throw new RuntimeException("Debe indicar al menos una línea de dispensación");
        }
        OrdenClinica orden = ordenClinicaRepository.findById(dto.getOrdenId())
                .orElseThrow(() -> new RuntimeException("Orden clínica no encontrada: " + dto.getOrdenId()));
        if (!TIPO_MEDICAMENTO.equalsIgnoreCase(orden.getTipo()) && !"COMPUESTA".equalsIgnoreCase(orden.getTipo())) {
            throw new RuntimeException("La orden no es de tipo MEDICAMENTO ni COMPUESTA con medicamentos");
        }
        Paciente paciente = orden.getPaciente();
        if (paciente == null) throw new RuntimeException("Orden sin paciente asociado");

        List<FarmaciaDispensacionDto> result = new ArrayList<>();
        for (LineaDispensacionDto linea : dto.getLineas()) {
            FarmaciaMedicamento med = medicamentoRepository.findById(linea.getMedicamentoId())
                    .orElseThrow(() -> new RuntimeException("Medicamento no encontrado: " + linea.getMedicamentoId()));
            if (linea.getCantidad() == null || linea.getCantidad() < 1) continue;
            if (med.getCantidad() < linea.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para " + med.getNombre() + " (disponible: " + med.getCantidad() + ")");
            }
            med.setCantidad(med.getCantidad() - linea.getCantidad());
            medicamentoRepository.save(med);

            FarmaciaDispensacion disp = FarmaciaDispensacion.builder()
                    .medicamento(med)
                    .paciente(paciente)
                    .cantidad(linea.getCantidad())
                    .ordenClinica(orden)
                    .build();
            result.add(toDispensacionDto(dispensacionRepository.save(disp)));
        }
        orden.setEstadoDispensacionFarmacia("PARCIAL");
        ordenClinicaRepository.save(orden);
        return result;
    }

    private OrdenFarmaciaPendienteDto toOrdenFarmaciaPendienteDto(OrdenClinica o) {
        String pacienteNombre = (o.getPaciente().getNombres() + " " + (o.getPaciente().getApellidos() != null ? o.getPaciente().getApellidos() : "")).trim();
        String medicoNombre = null;
        if (o.getConsulta() != null && o.getConsulta().getProfesional() != null) {
            var p = o.getConsulta().getProfesional();
            medicoNombre = (p.getNombres() + " " + (p.getApellidos() != null ? p.getApellidos() : "")).trim();
        }
        String alergiasPaciente = historiaClinicaRepository.findByPacienteId(o.getPaciente().getId())
                .map(hc -> hc.getAlergiasGenerales())
                .filter(a -> a != null && !a.isBlank())
                .orElse(null);

        List<OrdenFarmaciaPendienteItemDto> itemsDto = new ArrayList<>();
        if (o.getItems() != null && !o.getItems().isEmpty()) {
            for (OrdenClinicaItem it : o.getItems()) {
                if (!TIPO_MEDICAMENTO.equalsIgnoreCase(it.getTipo())) continue;
                itemsDto.add(OrdenFarmaciaPendienteItemDto.builder()
                        .id(it.getId())
                        .detalle(it.getDetalle())
                        .cantidadPrescrita(it.getCantidadPrescrita())
                        .unidadMedida(it.getUnidadMedida())
                        .frecuencia(it.getFrecuencia())
                        .duracionDias(it.getDuracionDias())
                        .build());
            }
        }
        if (itemsDto.isEmpty() && TIPO_MEDICAMENTO.equalsIgnoreCase(o.getTipo())) {
            itemsDto.add(OrdenFarmaciaPendienteItemDto.builder()
                    .id(null)
                    .detalle(o.getDetalle())
                    .cantidadPrescrita(o.getCantidadPrescrita())
                    .unidadMedida(o.getUnidadMedida())
                    .frecuencia(o.getFrecuencia())
                    .duracionDias(o.getDuracionDias())
                    .build());
        }

        OrdenFarmaciaPendienteItemDto first = itemsDto.isEmpty() ? null : itemsDto.get(0);
        return OrdenFarmaciaPendienteDto.builder()
                .id(o.getId())
                .pacienteId(o.getPaciente().getId())
                .pacienteNombre(pacienteNombre)
                .pacienteDocumento(o.getPaciente().getDocumento())
                .tipoDocumentoPaciente(o.getPaciente().getTipoDocumento())
                .alergiasPaciente(alergiasPaciente)
                .detalle(first != null ? first.getDetalle() : o.getDetalle())
                .cantidadPrescrita(first != null ? first.getCantidadPrescrita() : o.getCantidadPrescrita())
                .unidadMedida(first != null ? first.getUnidadMedida() : o.getUnidadMedida())
                .frecuencia(first != null ? first.getFrecuencia() : o.getFrecuencia())
                .duracionDias(first != null ? first.getDuracionDias() : o.getDuracionDias())
                .fechaOrden(o.getCreatedAt())
                .medicoNombre(medicoNombre)
                .estadoDispensacionFarmacia(o.getEstadoDispensacionFarmacia() != null ? o.getEstadoDispensacionFarmacia() : "PENDIENTE")
                .items(itemsDto.isEmpty() ? null : itemsDto)
                .build();
    }

    private FarmaciaMedicamentoDto toMedicamentoDto(FarmaciaMedicamento m) {
        return FarmaciaMedicamentoDto.builder()
                .id(m.getId())
                .nombre(m.getNombre())
                .lote(m.getLote())
                .codigoBarras(m.getCodigoBarras())
                .fechaVencimiento(m.getFechaVencimiento())
                .cantidad(m.getCantidad())
                .precio(m.getPrecio())
                .stockMinimo(m.getStockMinimo())
                .activo(m.getActivo())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private FarmaciaDispensacionDto toDispensacionDto(FarmaciaDispensacion d) {
        String pacienteNombre = d.getPaciente().getNombres() + " " +
                (d.getPaciente().getApellidos() != null ? d.getPaciente().getApellidos() : "");
        return FarmaciaDispensacionDto.builder()
                .id(d.getId())
                .medicamentoId(d.getMedicamento().getId())
                .medicamentoNombre(d.getMedicamento().getNombre())
                .pacienteId(d.getPaciente().getId())
                .pacienteNombre(pacienteNombre.trim())
                .cantidad(d.getCantidad())
                .fechaDispensacion(d.getFechaDispensacion())
                .entregadoPor(d.getEntregadoPor())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
