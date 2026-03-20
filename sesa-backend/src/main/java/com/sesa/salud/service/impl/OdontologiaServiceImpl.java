/**
 * Implementación del servicio Odontología.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.odontologia.*;
import com.sesa.salud.entity.*;
import com.sesa.salud.repository.*;
import com.sesa.salud.service.OdontologiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OdontologiaServiceImpl implements OdontologiaService {

    private final ConsultaOdontologicaRepository consultaRepo;
    private final OdontogramaEstadoRepository odontogramaRepo;
    private final ProcedimientoCatalogoRepository catalogoRepo;
    private final PlanTratamientoRepository planRepo;
    private final ImagenClinicaRepository imagenRepo;
    private final EvolucionOdontologicaRepository evolucionRepo;
    private final PacienteRepository pacienteRepo;
    private final PersonalRepository personalRepo;
    private final CitaRepository citaRepo;

    // ── Consultas ────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public List<ConsultaOdontologicaDto> getConsultasByPaciente(Long pacienteId, Pageable pageable) {
        return consultaRepo.findByPaciente_IdOrderByCreatedAtDesc(pacienteId, pageable)
                .stream().map(this::toConsultaDto).collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    public ConsultaOdontologicaDto getConsultaById(Long id) {
        return toConsultaDto(consultaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Consulta no encontrada: " + id)));
    }

    @Override @Transactional
    public ConsultaOdontologicaDto crearConsulta(ConsultaOdontologicaDto dto) {
        Paciente p = pacienteRepo.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));
        Personal prof = personalRepo.findById(dto.getProfesionalId())
                .orElseThrow(() -> new RuntimeException("Profesional no encontrado"));
        ConsultaOdontologica c = ConsultaOdontologica.builder()
                .paciente(p).profesional(prof)
                .tipoConsulta(dto.getTipoConsulta())
                .codigoCie10(dto.getCodigoCie10())
                .descripcionCie10(dto.getDescripcionCie10())
                .consentimientoFirmado(dto.getConsentimientoFirmado())
                .fechaConsentimiento(dto.getFechaConsentimiento())
                .consentimientoObservaciones(dto.getConsentimientoObservaciones())
                .motivoConsulta(dto.getMotivoConsulta())
                .enfermedadActual(dto.getEnfermedadActual())
                .antecedentesOdontologicos(dto.getAntecedentesOdontologicos())
                .antecedentesSistemicos(dto.getAntecedentesSistemicos())
                .medicamentosActuales(dto.getMedicamentosActuales())
                .alergias(dto.getAlergias())
                .habitosOrales(dto.getHabitosOrales())
                .higieneOral(dto.getHigieneOral())
                .examenExtraOral(dto.getExamenExtraOral())
                .examenIntraOral(dto.getExamenIntraOral())
                .cpodCariados(dto.getCpodCariados())
                .cpodPerdidos(dto.getCpodPerdidos())
                .cpodObturados(dto.getCpodObturados())
                .ceodCariados(dto.getCeodCariados())
                .ceodExtraidos(dto.getCeodExtraidos())
                .ceodObturados(dto.getCeodObturados())
                .ihosPlaca(dto.getIhosPlaca())
                .ihosCalculo(dto.getIhosCalculo())
                .condicionPeriodontal(dto.getCondicionPeriodontal())
                .riesgoCaries(dto.getRiesgoCaries())
                .diagnostico(dto.getDiagnostico())
                .planTratamiento(dto.getPlanTratamiento())
                .firmaProfesionalUrl(dto.getFirmaProfesionalUrl())
                .firmaCanvasData(dto.getFirmaCanvasData())
                .estado(dto.getEstado() != null ? dto.getEstado() : "EN_ATENCION")
                .build();
        if (dto.getCitaId() != null) {
            citaRepo.findById(dto.getCitaId()).ifPresent(c::setCita);
        }
        return toConsultaDto(consultaRepo.save(c));
    }

    @Override @Transactional
    public ConsultaOdontologicaDto actualizarConsulta(Long id, ConsultaOdontologicaDto dto) {
        ConsultaOdontologica c = consultaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Consulta no encontrada: " + id));
        c.setTipoConsulta(dto.getTipoConsulta());
        c.setCodigoCie10(dto.getCodigoCie10());
        c.setDescripcionCie10(dto.getDescripcionCie10());
        c.setConsentimientoFirmado(dto.getConsentimientoFirmado());
        c.setFechaConsentimiento(dto.getFechaConsentimiento());
        c.setConsentimientoObservaciones(dto.getConsentimientoObservaciones());
        c.setMotivoConsulta(dto.getMotivoConsulta());
        c.setEnfermedadActual(dto.getEnfermedadActual());
        c.setAntecedentesOdontologicos(dto.getAntecedentesOdontologicos());
        c.setAntecedentesSistemicos(dto.getAntecedentesSistemicos());
        c.setMedicamentosActuales(dto.getMedicamentosActuales());
        c.setAlergias(dto.getAlergias());
        c.setHabitosOrales(dto.getHabitosOrales());
        c.setHigieneOral(dto.getHigieneOral());
        c.setExamenExtraOral(dto.getExamenExtraOral());
        c.setExamenIntraOral(dto.getExamenIntraOral());
        c.setCpodCariados(dto.getCpodCariados());
        c.setCpodPerdidos(dto.getCpodPerdidos());
        c.setCpodObturados(dto.getCpodObturados());
        c.setCeodCariados(dto.getCeodCariados());
        c.setCeodExtraidos(dto.getCeodExtraidos());
        c.setCeodObturados(dto.getCeodObturados());
        c.setIhosPlaca(dto.getIhosPlaca());
        c.setIhosCalculo(dto.getIhosCalculo());
        c.setCondicionPeriodontal(dto.getCondicionPeriodontal());
        c.setRiesgoCaries(dto.getRiesgoCaries());
        c.setDiagnostico(dto.getDiagnostico());
        c.setPlanTratamiento(dto.getPlanTratamiento());
        c.setFirmaProfesionalUrl(dto.getFirmaProfesionalUrl());
        c.setFirmaCanvasData(dto.getFirmaCanvasData());
        if (dto.getEstado() != null) c.setEstado(dto.getEstado());
        return toConsultaDto(consultaRepo.save(c));
    }

    // ── Odontograma ──────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public List<OdontogramaEstadoDto> getOdontograma(Long pacienteId) {
        return odontogramaRepo.findEstadoActualByPacienteId(pacienteId)
                .stream().map(this::toOdontogramaDto).collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    public List<OdontogramaEstadoDto> getOdontogramaByConsulta(Long consultaId) {
        return odontogramaRepo.findByConsulta_IdOrderByPiezaFdiAsc(consultaId)
                .stream().map(this::toOdontogramaDto).collect(Collectors.toList());
    }

    @Override @Transactional
    public OdontogramaEstadoDto guardarEstadoPieza(OdontogramaEstadoDto dto) {
        return toOdontogramaDto(odontogramaRepo.save(fromOdontogramaDto(dto)));
    }

    @Override @Transactional
    public List<OdontogramaEstadoDto> guardarOdontogramaBatch(List<OdontogramaEstadoDto> cambios) {
        List<OdontogramaEstado> entities = cambios.stream().map(this::fromOdontogramaDto).collect(Collectors.toList());
        return odontogramaRepo.saveAll(entities).stream().map(this::toOdontogramaDto).collect(Collectors.toList());
    }

    // ── Catálogo ─────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public List<ProcedimientoCatalogo> getCatalogoProcedimientos() {
        return catalogoRepo.findByActivoTrueOrderByCategoria();
    }

    @Override @Transactional
    public ProcedimientoCatalogo crearProcedimientoCatalogo(ProcedimientoCatalogo dto) {
        return catalogoRepo.save(dto);
    }

    @Override @Transactional
    public ProcedimientoCatalogo actualizarProcedimientoCatalogo(Long id, ProcedimientoCatalogo dto) {
        ProcedimientoCatalogo existing = catalogoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Procedimiento no encontrado: " + id));
        existing.setCodigo(dto.getCodigo());
        existing.setNombre(dto.getNombre());
        existing.setDescripcion(dto.getDescripcion());
        existing.setCategoria(dto.getCategoria());
        existing.setPrecioBase(dto.getPrecioBase());
        existing.setActivo(dto.getActivo());
        return catalogoRepo.save(existing);
    }

    // ── Planes ───────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public List<PlanTratamientoDto> getPlanesByPaciente(Long pacienteId) {
        return planRepo.findByPaciente_IdOrderByCreatedAtDesc(pacienteId)
                .stream().map(this::toPlanDto).collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    public PlanTratamientoDto getPlanById(Long id) {
        return toPlanDto(planRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado: " + id)));
    }

    @Override @Transactional
    public PlanTratamientoDto crearPlan(PlanTratamientoDto dto) {
        Paciente p = pacienteRepo.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));
        Personal prof = personalRepo.findById(dto.getProfesionalId())
                .orElseThrow(() -> new RuntimeException("Profesional no encontrado"));
        PlanTratamiento plan = PlanTratamiento.builder()
                .paciente(p).profesional(prof)
                .nombre(dto.getNombre() != null ? dto.getNombre() : "Plan de Tratamiento")
                .fase(dto.getFase() != null ? dto.getFase() : 1)
                .descripcion(dto.getDescripcion())
                .valorTotal(dto.getValorTotal() != null ? dto.getValorTotal() : BigDecimal.ZERO)
                .descuento(dto.getDescuento() != null ? dto.getDescuento() : BigDecimal.ZERO)
                .valorFinal(dto.getValorFinal() != null ? dto.getValorFinal() : BigDecimal.ZERO)
                .tipoPago(dto.getTipoPago() != null ? dto.getTipoPago() : "PARTICULAR")
                .estado("PENDIENTE")
                .fechaInicio(dto.getFechaInicio() != null ? dto.getFechaInicio() : LocalDate.now())
                .fechaFin(dto.getFechaFin())
                .build();
        if (dto.getConsultaId() != null) {
            consultaRepo.findById(dto.getConsultaId()).ifPresent(plan::setConsulta);
        }
        if (dto.getItems() != null) {
            for (PlanTratamientoItemDto itemDto : dto.getItems()) {
                ProcedimientoCatalogo proc = catalogoRepo.findById(itemDto.getProcedimientoId())
                        .orElseThrow(() -> new RuntimeException("Procedimiento no encontrado"));
                PlanTratamientoItem item = PlanTratamientoItem.builder()
                        .plan(plan).procedimiento(proc)
                        .piezaFdi(itemDto.getPiezaFdi())
                        .cantidad(itemDto.getCantidad() != null ? itemDto.getCantidad() : 1)
                        .precioUnitario(itemDto.getPrecioUnitario())
                        .descuento(itemDto.getDescuento() != null ? itemDto.getDescuento() : BigDecimal.ZERO)
                        .valorTotal(itemDto.getValorTotal())
                        .estado("PENDIENTE")
                        .observaciones(itemDto.getObservaciones())
                        .build();
                plan.getItems().add(item);
            }
        }
        return toPlanDto(planRepo.save(plan));
    }

    @Override @Transactional
    public PlanTratamientoDto actualizarPlan(Long id, PlanTratamientoDto dto) {
        PlanTratamiento plan = planRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado: " + id));
        plan.setNombre(dto.getNombre());
        plan.setFase(dto.getFase());
        plan.setDescripcion(dto.getDescripcion());
        plan.setValorTotal(dto.getValorTotal());
        plan.setDescuento(dto.getDescuento());
        plan.setValorFinal(dto.getValorFinal());
        plan.setTipoPago(dto.getTipoPago());
        plan.setFechaFin(dto.getFechaFin());
        if (dto.getEstado() != null) plan.setEstado(dto.getEstado());
        return toPlanDto(planRepo.save(plan));
    }

    @Override @Transactional
    public PlanTratamientoDto cambiarEstadoPlan(Long id, String estado) {
        PlanTratamiento plan = planRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado: " + id));
        plan.setEstado(estado);
        return toPlanDto(planRepo.save(plan));
    }

    @Override @Transactional
    public PlanTratamientoDto registrarAbono(Long id, BigDecimal monto) {
        PlanTratamiento plan = planRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado: " + id));
        plan.setValorAbonado(plan.getValorAbonado().add(monto));
        if (plan.getValorAbonado().compareTo(plan.getValorFinal()) >= 0) {
            plan.setEstado("FINALIZADO");
        } else if ("PENDIENTE".equals(plan.getEstado())) {
            plan.setEstado("EN_TRATAMIENTO");
        }
        return toPlanDto(planRepo.save(plan));
    }

    // ── Imágenes ─────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public List<ImagenClinicaDto> getImagenesByPaciente(Long pacienteId) {
        return imagenRepo.findByPaciente_IdOrderByCreatedAtDesc(pacienteId)
                .stream().map(this::toImagenDto).collect(Collectors.toList());
    }

    @Override @Transactional
    public ImagenClinicaDto subirImagen(ImagenClinicaDto dto) {
        Paciente p = pacienteRepo.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));
        Personal prof = personalRepo.findById(dto.getProfesionalId())
                .orElseThrow(() -> new RuntimeException("Profesional no encontrado"));
        ImagenClinica img = ImagenClinica.builder()
                .paciente(p).profesional(prof)
                .piezaFdi(dto.getPiezaFdi())
                .tipo(dto.getTipo() != null ? dto.getTipo() : "FOTO_CLINICA")
                .nombreArchivo(dto.getNombreArchivo())
                .url(dto.getUrl())
                .thumbnailBase64(dto.getThumbnailBase64())
                .descripcion(dto.getDescripcion())
                .build();
        if (dto.getConsultaId() != null) {
            consultaRepo.findById(dto.getConsultaId()).ifPresent(img::setConsulta);
        }
        return toImagenDto(imagenRepo.save(img));
    }

    @Override @Transactional
    public void eliminarImagen(Long id) {
        imagenRepo.deleteById(id);
    }

    // ── Evoluciones ──────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public List<EvolucionOdontologicaDto> getEvolucionesByPaciente(Long pacienteId) {
        return evolucionRepo.findByPaciente_IdOrderByCreatedAtDesc(pacienteId)
                .stream().map(this::toEvolucionDto).collect(Collectors.toList());
    }

    @Override @Transactional
    public EvolucionOdontologicaDto registrarEvolucion(EvolucionOdontologicaDto dto) {
        Paciente p = pacienteRepo.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));
        Personal prof = personalRepo.findById(dto.getProfesionalId())
                .orElseThrow(() -> new RuntimeException("Profesional no encontrado"));
        ConsultaOdontologica consulta = consultaRepo.findById(dto.getConsultaId())
                .orElseThrow(() -> new RuntimeException("Consulta no encontrada"));
        EvolucionOdontologica ev = EvolucionOdontologica.builder()
                .paciente(p).profesional(prof).consulta(consulta)
                .notaEvolucion(dto.getNotaEvolucion())
                .controlPostTratamiento(dto.getControlPostTratamiento())
                .proximaCitaRecomendada(dto.getProximaCitaRecomendada())
                .build();
        if (dto.getPlanId() != null) {
            planRepo.findById(dto.getPlanId()).ifPresent(ev::setPlan);
        }
        return toEvolucionDto(evolucionRepo.save(ev));
    }

    // ── Stats ────────────────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public Map<String, Object> getStatsDelDia(Long profesionalId) {
        long totalConsultas = consultaRepo.findByProfesionalId(profesionalId,
                org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();
        long planesActivos = planRepo.findByPaciente_IdAndEstadoOrderByFase(-1L, "EN_TRATAMIENTO").size();
        return Map.of(
                "totalConsultasHoy", 0L,
                "totalConsultasHistorico", totalConsultas,
                "planesActivos", planesActivos
        );
    }

    // ── Conversores ──────────────────────────────────────────────────────

    private ConsultaOdontologicaDto toConsultaDto(ConsultaOdontologica c) {
        Paciente p = c.getPaciente();
        String nombre = (p.getNombres() + " " + (p.getApellidos() != null ? p.getApellidos() : "")).trim();
        Integer edad = p.getFechaNacimiento() != null ? Period.between(p.getFechaNacimiento(), LocalDate.now()).getYears() : null;
        return ConsultaOdontologicaDto.builder()
                .id(c.getId())
                .pacienteId(p.getId()).pacienteNombre(nombre)
                .pacienteDocumento(p.getDocumento()).pacienteEdad(edad)
                .pacienteEps(p.getEps() != null ? p.getEps().getNombre() : null)
                .profesionalId(c.getProfesional().getId())
                .profesionalNombre((c.getProfesional().getNombres() + " " + (c.getProfesional().getApellidos() != null ? c.getProfesional().getApellidos() : "")).trim())
                .citaId(c.getCita() != null ? c.getCita().getId() : null)
                .tipoConsulta(c.getTipoConsulta())
                .codigoCie10(c.getCodigoCie10()).descripcionCie10(c.getDescripcionCie10())
                .consentimientoFirmado(c.getConsentimientoFirmado())
                .fechaConsentimiento(c.getFechaConsentimiento())
                .consentimientoObservaciones(c.getConsentimientoObservaciones())
                .motivoConsulta(c.getMotivoConsulta()).enfermedadActual(c.getEnfermedadActual())
                .antecedentesOdontologicos(c.getAntecedentesOdontologicos())
                .antecedentesSistemicos(c.getAntecedentesSistemicos())
                .medicamentosActuales(c.getMedicamentosActuales())
                .alergias(c.getAlergias()).habitosOrales(c.getHabitosOrales())
                .higieneOral(c.getHigieneOral()).examenExtraOral(c.getExamenExtraOral())
                .examenIntraOral(c.getExamenIntraOral())
                .cpodCariados(c.getCpodCariados()).cpodPerdidos(c.getCpodPerdidos()).cpodObturados(c.getCpodObturados())
                .ceodCariados(c.getCeodCariados()).ceodExtraidos(c.getCeodExtraidos()).ceodObturados(c.getCeodObturados())
                .ihosPlaca(c.getIhosPlaca()).ihosCalculo(c.getIhosCalculo())
                .condicionPeriodontal(c.getCondicionPeriodontal()).riesgoCaries(c.getRiesgoCaries())
                .diagnostico(c.getDiagnostico()).planTratamiento(c.getPlanTratamiento())
                .firmaProfesionalUrl(c.getFirmaProfesionalUrl()).firmaCanvasData(c.getFirmaCanvasData())
                .estado(c.getEstado()).createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt())
                .build();
    }

    private OdontogramaEstadoDto toOdontogramaDto(OdontogramaEstado o) {
        Personal prof = o.getProfesional();
        return OdontogramaEstadoDto.builder()
                .id(o.getId()).pacienteId(o.getPaciente().getId())
                .profesionalId(prof.getId())
                .profesionalNombre((prof.getNombres() + " " + (prof.getApellidos() != null ? prof.getApellidos() : "")).trim())
                .consultaId(o.getConsulta() != null ? o.getConsulta().getId() : null)
                .piezaFdi(o.getPiezaFdi()).superficie(o.getSuperficie())
                .estado(o.getEstado()).observacion(o.getObservacion())
                .createdAt(o.getCreatedAt()).build();
    }

    private OdontogramaEstado fromOdontogramaDto(OdontogramaEstadoDto dto) {
        Paciente p = pacienteRepo.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));
        Personal prof = personalRepo.findById(dto.getProfesionalId())
                .orElseThrow(() -> new RuntimeException("Profesional no encontrado"));
        OdontogramaEstado o = OdontogramaEstado.builder()
                .paciente(p).profesional(prof)
                .piezaFdi(dto.getPiezaFdi()).superficie(dto.getSuperficie())
                .estado(dto.getEstado()).observacion(dto.getObservacion()).build();
        if (dto.getConsultaId() != null) {
            consultaRepo.findById(dto.getConsultaId()).ifPresent(o::setConsulta);
        }
        return o;
    }

    private PlanTratamientoDto toPlanDto(PlanTratamiento plan) {
        BigDecimal saldo = plan.getValorFinal().subtract(plan.getValorAbonado());
        List<PlanTratamientoItemDto> items = plan.getItems().stream().map(i ->
                PlanTratamientoItemDto.builder()
                        .id(i.getId()).planId(plan.getId())
                        .procedimientoId(i.getProcedimiento().getId())
                        .procedimientoNombre(i.getProcedimiento().getNombre())
                        .procedimientoCodigo(i.getProcedimiento().getCodigo())
                        .piezaFdi(i.getPiezaFdi()).cantidad(i.getCantidad())
                        .precioUnitario(i.getPrecioUnitario()).descuento(i.getDescuento())
                        .valorTotal(i.getValorTotal()).estado(i.getEstado())
                        .observaciones(i.getObservaciones()).createdAt(i.getCreatedAt())
                        .build()).collect(Collectors.toList());
        return PlanTratamientoDto.builder()
                .id(plan.getId()).pacienteId(plan.getPaciente().getId())
                .pacienteNombre((plan.getPaciente().getNombres() + " " + (plan.getPaciente().getApellidos() != null ? plan.getPaciente().getApellidos() : "")).trim())
                .profesionalId(plan.getProfesional().getId())
                .profesionalNombre((plan.getProfesional().getNombres() + " " + (plan.getProfesional().getApellidos() != null ? plan.getProfesional().getApellidos() : "")).trim())
                .consultaId(plan.getConsulta() != null ? plan.getConsulta().getId() : null)
                .nombre(plan.getNombre()).fase(plan.getFase()).descripcion(plan.getDescripcion())
                .valorTotal(plan.getValorTotal()).descuento(plan.getDescuento())
                .valorFinal(plan.getValorFinal()).valorAbonado(plan.getValorAbonado())
                .saldoPendiente(saldo.max(BigDecimal.ZERO))
                .tipoPago(plan.getTipoPago()).estado(plan.getEstado())
                .fechaInicio(plan.getFechaInicio()).fechaFin(plan.getFechaFin())
                .items(items).createdAt(plan.getCreatedAt()).updatedAt(plan.getUpdatedAt())
                .build();
    }

    private ImagenClinicaDto toImagenDto(ImagenClinica i) {
        return ImagenClinicaDto.builder()
                .id(i.getId()).pacienteId(i.getPaciente().getId())
                .profesionalId(i.getProfesional().getId())
                .profesionalNombre((i.getProfesional().getNombres() + " " + (i.getProfesional().getApellidos() != null ? i.getProfesional().getApellidos() : "")).trim())
                .consultaId(i.getConsulta() != null ? i.getConsulta().getId() : null)
                .piezaFdi(i.getPiezaFdi()).tipo(i.getTipo())
                .nombreArchivo(i.getNombreArchivo()).url(i.getUrl())
                .thumbnailBase64(i.getThumbnailBase64()).descripcion(i.getDescripcion())
                .createdAt(i.getCreatedAt()).build();
    }

    private EvolucionOdontologicaDto toEvolucionDto(EvolucionOdontologica e) {
        return EvolucionOdontologicaDto.builder()
                .id(e.getId()).pacienteId(e.getPaciente().getId())
                .profesionalId(e.getProfesional().getId())
                .profesionalNombre((e.getProfesional().getNombres() + " " + (e.getProfesional().getApellidos() != null ? e.getProfesional().getApellidos() : "")).trim())
                .consultaId(e.getConsulta().getId())
                .planId(e.getPlan() != null ? e.getPlan().getId() : null)
                .notaEvolucion(e.getNotaEvolucion())
                .controlPostTratamiento(e.getControlPostTratamiento())
                .proximaCitaRecomendada(e.getProximaCitaRecomendada())
                .createdAt(e.getCreatedAt()).build();
    }
}
