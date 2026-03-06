/**
 * Servicio de generación de PDF premium para Historia Clínica
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sesa.salud.dto.EmpresaDto;
import com.sesa.salud.dto.pdf.*;
import com.sesa.salud.entity.*;
import com.sesa.salud.repository.ConsultaRepository;
import com.sesa.salud.repository.HistoriaClinicaRepository;
import com.sesa.salud.repository.OrdenClinicaRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoriaClinicaPdfService {

    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final ConsultaRepository consultaRepository;
    private final OrdenClinicaRepository ordenClinicaRepository;
    private final PacienteRepository pacienteRepository;
    private final EmpresaService empresaService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Parsea resultado en líneas "Etiqueta: valor" a ítems para PDF (etiqueta en negrita + valor). */
    private List<ResultadoItemPdfDto> parseResultadoItems(String resultado) {
        if (resultado == null || resultado.isBlank()) return List.of();
        List<ResultadoItemPdfDto> items = new ArrayList<>();
        for (String line : resultado.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            int colon = trimmed.indexOf(": ");
            if (colon > 0) {
                items.add(ResultadoItemPdfDto.builder()
                        .etiqueta(trimmed.substring(0, colon).trim())
                        .valor(trimmed.substring(colon + 2).trim())
                        .build());
            } else {
                items.add(ResultadoItemPdfDto.builder().etiqueta(trimmed).valor("").build());
            }
        }
        return items;
    }

    /** Engine standalone (no requiere contexto web de Spring). */
    private static final TemplateEngine TEMPLATE_ENGINE = buildEngine();

    private static TemplateEngine buildEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    /* ── Puntos de entrada ──────────────────────────────────────────── */

    @Transactional(readOnly = true)
    public byte[] generarPdf(Long historiaId) {
        HistoriaClinica historia = historiaClinicaRepository.findById(historiaId)
                .orElseThrow(() -> new RuntimeException("Historia clínica no encontrada: " + historiaId));
        return renderizar(historia);
    }

    @Transactional(readOnly = true)
    public byte[] generarPdfPorPaciente(Long pacienteId) {
        HistoriaClinica historia = historiaClinicaRepository.findByPacienteId(pacienteId)
                .orElseThrow(() -> new RuntimeException("Historia clínica no encontrada para paciente: " + pacienteId));
        return renderizar(historia);
    }

    /** PDF de órdenes clínicas y resultados del paciente. */
    @Transactional(readOnly = true)
    public byte[] generarPdfOrdenesPaciente(Long pacienteId) {
        OrdenesPacientePdfDto dto = buildOrdenesPacienteDto(pacienteId);
        String html = TEMPLATE_ENGINE.process("pdf/ordenes-resultados", buildOrdenesContext(dto));
        return htmlToPdf(html);
    }

    /** PDF de una sola orden clínica (plantilla según tipo: laboratorio, medicamento, procedimiento). */
    @Transactional(readOnly = true)
    public byte[] generarPdfOrdenIndividual(Long ordenId) {
        OrdenesPacientePdfDto dto = buildOrdenIndividualDto(ordenId);
        String template = templateOrdenIndividualPorTipo(dto);
        String html = TEMPLATE_ENGINE.process(template, buildOrdenesContext(dto));
        return htmlToPdf(html);
    }

    /** Devuelve la plantilla PDF según el tipo de la primera orden (laboratorio, medicamento, procedimiento/imagen). */
    private String templateOrdenIndividualPorTipo(OrdenesPacientePdfDto dto) {
        if (dto == null || dto.getOrdenes() == null || dto.getOrdenes().isEmpty())
            return "pdf/ordenes-resultados";
        String tipo = dto.getOrdenes().get(0).getTipo();
        if (tipo != null) {
            switch (tipo.toUpperCase()) {
                case "MEDICAMENTO": return "pdf/orden-medicamento";
                case "PROCEDIMIENTO":
                case "IMAGEN": return "pdf/orden-procedimiento";
                case "LABORATORIO":
                default: return "pdf/ordenes-resultados";
            }
        }
        return "pdf/ordenes-resultados";
    }

    private OrdenesPacientePdfDto buildOrdenIndividualDto(Long ordenId) {
        OrdenClinica oc = ordenClinicaRepository.findById(ordenId)
                .orElseThrow(() -> new RuntimeException("Orden clínica no encontrada: " + ordenId));
        Paciente p = oc.getPaciente();
        if (p == null) throw new RuntimeException("Orden sin paciente asociado: " + ordenId);
        EmpresaDto empresa = loadEmpresa();
        String logoBase64 = null;
        String logoContentType = null;
        try {
            var logoOpt = empresaService.getLogoResource(TenantContextHolder.getTenantSchema());
            if (logoOpt.isPresent()) {
                byte[] logoBytes = logoOpt.get().getResource().getContentAsByteArray();
                logoBase64 = Base64.getEncoder().encodeToString(logoBytes);
                logoContentType = logoOpt.get().getContentType() != null ? logoOpt.get().getContentType() : "image/png";
            }
        } catch (Exception e) {
            log.debug("No se pudo cargar logo para PDF orden individual: {}", e.getMessage());
        }
        String fechaRes = oc.getFechaResultado() != null ? DT_FMT.format(oc.getFechaResultado().atZone(ZoneOffset.UTC)) : null;
        OrdenPdfDto ordenDto = OrdenPdfDto.builder()
                .idOrden(oc.getId())
                .tipo(oc.getTipo())
                .detalle(oc.getDetalle())
                .cantidadPrescrita(oc.getCantidadPrescrita())
                .unidadMedida(oc.getUnidadMedida())
                .frecuencia(oc.getFrecuencia())
                .duracionDias(oc.getDuracionDias())
                .estado(oc.getEstado() != null ? oc.getEstado() : "PENDIENTE")
                .resultado(oc.getResultado())
                .fechaResultado(fechaRes)
                .resultadoItems(parseResultadoItems(oc.getResultado()))
                .build();
        List<OrdenPdfDto> ordenesDtos = List.of(ordenDto);
        String pacienteNombre = (p.getNombres() + " " + (p.getApellidos() != null ? p.getApellidos() : "")).trim();
        String pacienteFechaNac = p.getFechaNacimiento() != null ? DATE_FMT.format(p.getFechaNacimiento()) : null;
        String pacienteEdad = p.getFechaNacimiento() != null ? String.valueOf(Period.between(p.getFechaNacimiento(), LocalDate.now()).getYears()) : null;
        String profesionalNombre = null;
        String profesionalRol = null;
        String profesionalIdentificacion = null;
        String profesionalTarjeta = null;
        String profesionalFirmaBase64 = null;
        String profesionalFirmaContentType = null;
        Personal prof = oc.getResultadoRegistradoPor();
        if (prof == null && oc.getConsulta() != null && oc.getConsulta().getProfesional() != null) {
            prof = oc.getConsulta().getProfesional();
        }
        if (prof != null) {
            profesionalNombre = (prof.getNombres() + " " + (prof.getApellidos() != null ? prof.getApellidos() : "")).trim();
            profesionalRol = prof.getRol();
            profesionalIdentificacion = prof.getIdentificacion();
            profesionalTarjeta = prof.getTarjetaProfesional();
            if (prof.getFirmaData() != null && prof.getFirmaData().length > 0) {
                profesionalFirmaBase64 = Base64.getEncoder().encodeToString(prof.getFirmaData());
                profesionalFirmaContentType = prof.getFirmaContentType() != null ? prof.getFirmaContentType() : "image/png";
            }
        }
        return OrdenesPacientePdfDto.builder()
                .empresaNombre(empresa != null ? empresa.getRazonSocial() : "IPS SESA Salud")
                .empresaIdentificacion(empresa != null ? empresa.getIdentificacion() : null)
                .logoBase64(logoBase64)
                .logoContentType(logoContentType)
                .pacienteNombre(pacienteNombre)
                .pacienteDocumento(p.getDocumento())
                .pacienteTipoDocumento(p.getTipoDocumento())
                .pacienteFechaNacimiento(pacienteFechaNac)
                .pacienteEdad(pacienteEdad)
                .pacienteSexo(p.getSexo())
                .pacienteTelefono(p.getTelefono())
                .pacienteEmail(p.getEmail())
                .pacienteDireccion(p.getDireccion())
                .fechaGeneracion(DT_FMT.format(java.time.ZonedDateTime.now(ZoneOffset.UTC)))
                .profesionalNombre(profesionalNombre)
                .profesionalRol(profesionalRol)
                .profesionalIdentificacion(profesionalIdentificacion)
                .profesionalTarjetaProfesional(profesionalTarjeta)
                .profesionalFirmaBase64(profesionalFirmaBase64)
                .profesionalFirmaContentType(profesionalFirmaContentType)
                .ordenes(ordenesDtos)
                .build();
    }

    private Context buildOrdenesContext(OrdenesPacientePdfDto dto) {
        Context ctx = new Context();
        ctx.setVariable("data", dto);
        if (dto != null) {
            ctx.setVariable("pacienteFechaNacimiento", dto.getPacienteFechaNacimiento());
            ctx.setVariable("pacienteEdad", dto.getPacienteEdad());
            ctx.setVariable("pacienteSexo", dto.getPacienteSexo());
            ctx.setVariable("pacienteTelefono", dto.getPacienteTelefono());
            ctx.setVariable("pacienteEmail", dto.getPacienteEmail());
            ctx.setVariable("pacienteDireccion", dto.getPacienteDireccion());
            ctx.setVariable("pacienteTipoDocumento", dto.getPacienteTipoDocumento());
            ctx.setVariable("profesionalNombre", dto.getProfesionalNombre());
            ctx.setVariable("profesionalRol", dto.getProfesionalRol());
            ctx.setVariable("profesionalIdentificacion", dto.getProfesionalIdentificacion());
            ctx.setVariable("profesionalTarjetaProfesional", dto.getProfesionalTarjetaProfesional());
            ctx.setVariable("profesionalFirmaBase64", dto.getProfesionalFirmaBase64());
            ctx.setVariable("profesionalFirmaContentType", dto.getProfesionalFirmaContentType());
        }
        return ctx;
    }

    private OrdenesPacientePdfDto buildOrdenesPacienteDto(Long pacienteId) {
        Paciente p = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + pacienteId));
        EmpresaDto empresa = loadEmpresa();
        String logoBase64 = null;
        String logoContentType = null;
        try {
            var logoOpt = empresaService.getLogoResource(TenantContextHolder.getTenantSchema());
            if (logoOpt.isPresent()) {
                byte[] logoBytes = logoOpt.get().getResource().getContentAsByteArray();
                logoBase64 = Base64.getEncoder().encodeToString(logoBytes);
                logoContentType = logoOpt.get().getContentType() != null ? logoOpt.get().getContentType() : "image/png";
            }
        } catch (Exception e) {
            log.debug("No se pudo cargar logo para PDF órdenes: {}", e.getMessage());
        }
        List<OrdenClinica> list = ordenClinicaRepository.findByPaciente_IdOrderByCreatedAtDesc(pacienteId, PageRequest.of(0, 500));
        if (list == null) list = new ArrayList<>();
        List<OrdenPdfDto> ordenesDtos = new ArrayList<>();
        Personal ultimoProfConResultado = null;
        for (OrdenClinica oc : list) {
            String fechaRes = oc.getFechaResultado() != null ? DT_FMT.format(oc.getFechaResultado().atZone(ZoneOffset.UTC)) : null;
            ordenesDtos.add(OrdenPdfDto.builder()
                    .idOrden(oc.getId())
                    .tipo(oc.getTipo())
                    .detalle(oc.getDetalle())
                    .cantidadPrescrita(oc.getCantidadPrescrita())
                    .unidadMedida(oc.getUnidadMedida())
                    .frecuencia(oc.getFrecuencia())
                    .duracionDias(oc.getDuracionDias())
                    .estado(oc.getEstado() != null ? oc.getEstado() : "PENDIENTE")
                    .resultado(oc.getResultado())
                    .fechaResultado(fechaRes)
                    .resultadoItems(parseResultadoItems(oc.getResultado()))
                    .build());
            if (oc.getResultadoRegistradoPor() != null) ultimoProfConResultado = oc.getResultadoRegistradoPor();
        }
        String pacienteNombre = (p.getNombres() + " " + (p.getApellidos() != null ? p.getApellidos() : "")).trim();
        String pacienteFechaNac = p.getFechaNacimiento() != null ? DATE_FMT.format(p.getFechaNacimiento()) : null;
        String pacienteEdad = p.getFechaNacimiento() != null ? String.valueOf(Period.between(p.getFechaNacimiento(), LocalDate.now()).getYears()) : null;
        String profesionalNombre = null;
        String profesionalRol = null;
        String profesionalIdentificacion = null;
        String profesionalTarjeta = null;
        String profesionalFirmaBase64 = null;
        String profesionalFirmaContentType = null;
        if (ultimoProfConResultado != null) {
            var prof = ultimoProfConResultado;
            profesionalNombre = (prof.getNombres() + " " + (prof.getApellidos() != null ? prof.getApellidos() : "")).trim();
            profesionalRol = prof.getRol();
            profesionalIdentificacion = prof.getIdentificacion();
            profesionalTarjeta = prof.getTarjetaProfesional();
            if (prof.getFirmaData() != null && prof.getFirmaData().length > 0) {
                profesionalFirmaBase64 = Base64.getEncoder().encodeToString(prof.getFirmaData());
                profesionalFirmaContentType = prof.getFirmaContentType() != null ? prof.getFirmaContentType() : "image/png";
            }
        }
        return OrdenesPacientePdfDto.builder()
                .empresaNombre(empresa != null ? empresa.getRazonSocial() : "IPS SESA Salud")
                .empresaIdentificacion(empresa != null ? empresa.getIdentificacion() : null)
                .logoBase64(logoBase64)
                .logoContentType(logoContentType)
                .pacienteNombre(pacienteNombre)
                .pacienteDocumento(p.getDocumento())
                .pacienteTipoDocumento(p.getTipoDocumento())
                .pacienteFechaNacimiento(pacienteFechaNac)
                .pacienteEdad(pacienteEdad)
                .pacienteSexo(p.getSexo())
                .pacienteTelefono(p.getTelefono())
                .pacienteEmail(p.getEmail())
                .pacienteDireccion(p.getDireccion())
                .fechaGeneracion(DT_FMT.format(java.time.ZonedDateTime.now(ZoneOffset.UTC)))
                .profesionalNombre(profesionalNombre)
                .profesionalRol(profesionalRol)
                .profesionalIdentificacion(profesionalIdentificacion)
                .profesionalTarjetaProfesional(profesionalTarjeta)
                .profesionalFirmaBase64(profesionalFirmaBase64)
                .profesionalFirmaContentType(profesionalFirmaContentType)
                .ordenes(ordenesDtos)
                .build();
    }

    private byte[] renderizar(HistoriaClinica hc) {
        HistoriaClinicaPdfDto dto = buildDto(hc);
        String html = renderTemplate(dto);
        return htmlToPdf(html);
    }

    /* ── Build DTO ──────────────────────────────────────────────────── */

    private HistoriaClinicaPdfDto buildDto(HistoriaClinica hc) {
        Paciente p = hc.getPaciente();

        EmpresaDto empresa = loadEmpresa();

        /* Logo en base64 */
        String logoBase64 = null;
        String logoContentType = null;
        try {
            var logoOpt = empresaService.getLogoResource(TenantContextHolder.getTenantSchema());
            if (logoOpt.isPresent()) {
                byte[] logoBytes = logoOpt.get().getResource().getContentAsByteArray();
                logoBase64 = Base64.getEncoder().encodeToString(logoBytes);
                logoContentType = logoOpt.get().getContentType() != null
                        ? logoOpt.get().getContentType() : "image/png";
            }
        } catch (Exception e) {
            log.debug("No se pudo cargar logo para PDF: {}", e.getMessage());
        }

        /* Edad */
        String edad = null;
        if (p.getFechaNacimiento() != null) {
            edad = Period.between(p.getFechaNacimiento(), LocalDate.now()).getYears() + " años";
        }

        /* Atenciones ordenadas desc */
        List<AtencionPdfDto> atencionesDtos = hc.getAtenciones().stream()
                .sorted((a, b) -> b.getFechaAtencion().compareTo(a.getFechaAtencion()))
                .map(this::toAtencionDto)
                .collect(Collectors.toList());

        /* Última consulta SOAP del paciente (modelo Consulta) */
        ConsultaSoapPdfDto ultimaSoap = buildUltimaConsultaSoap(p.getId());

        return HistoriaClinicaPdfDto.builder()
                .empresaNombre(empresa != null ? empresa.getRazonSocial() : "IPS SESA Salud")
                .empresaIdentificacion(empresa != null ? empresa.getIdentificacion() : null)
                .empresaTipoDocumento(empresa != null ? empresa.getTipoDocumento() : "NIT")
                .empresaDireccion(empresa != null ? empresa.getDireccionEmpresa() : null)
                .empresaTelefono(empresa != null ? empresa.getTelefono() : null)
                .empresaMunicipio(empresa != null ? empresa.getMunicipio() : null)
                .empresaDepartamento(empresa != null ? empresa.getDepartamento() : null)
                .empresaRegimen(empresa != null ? empresa.getRegimen() : null)
                .logoBase64(logoBase64)
                .logoContentType(logoContentType)
                .historiaId(hc.getId())
                .estadoHistoria(hc.getEstado())
                .fechaApertura(hc.getFechaApertura() != null
                        ? DATE_FMT.format(hc.getFechaApertura().atZone(ZoneOffset.UTC)) : null)
                .fechaGeneracion(DT_FMT.format(java.time.ZonedDateTime.now(ZoneOffset.UTC)))
                .pacienteNombre(
                        (p.getNombres() + " " + (p.getApellidos() != null ? p.getApellidos() : "")).trim())
                .pacienteDocumento(p.getDocumento())
                .pacienteTipoDocumento(p.getTipoDocumento() != null ? p.getTipoDocumento() : "CC")
                .pacienteFechaNacimiento(p.getFechaNacimiento() != null
                        ? DATE_FMT.format(p.getFechaNacimiento()) : null)
                .pacienteEdad(edad)
                .pacienteSexo(p.getSexo())
                .pacienteTelefono(p.getTelefono())
                .pacienteEmail(p.getEmail())
                .pacienteDireccion(p.getDireccion())
                .epsNombre(p.getEps() != null ? p.getEps().getNombre() : null)
                .epsCodigo(p.getEps() != null ? p.getEps().getCodigo() : null)
                .pacienteMunicipioResidencia(p.getMunicipioResidencia())
                .pacienteDepartamentoResidencia(p.getDepartamentoResidencia())
                .pacienteZonaResidencia(p.getZonaResidencia())
                .pacienteRegimenAfiliacion(p.getRegimenAfiliacion())
                .pacienteTipoUsuario(p.getTipoUsuario())
                .pacienteContactoEmergenciaNombre(p.getContactoEmergenciaNombre())
                .pacienteContactoEmergenciaTelefono(p.getContactoEmergenciaTelefono())
                .pacienteEstadoCivil(p.getEstadoCivil())
                .pacienteEscolaridad(p.getEscolaridad())
                .pacienteOcupacion(p.getOcupacion())
                .pacientePertenenciaEtnica(p.getPertenenciaEtnica())
                .grupoSanguineo(hc.getGrupoSanguineo())
                .alergiasGenerales(hc.getAlergiasGenerales())
                .antecedentesPersonales(hc.getAntecedentesPersonales())
                .antecedentesQuirurgicos(hc.getAntecedentesQuirurgicos())
                .antecedentesFarmacologicos(hc.getAntecedentesFarmacologicos())
                .antecedentesTraumaticos(hc.getAntecedentesTraumaticos())
                .antecedentesGinecoobstetricos(hc.getAntecedentesGinecoobstetricos())
                .antecedentesFamiliares(hc.getAntecedentesFamiliares())
                .habitosTabaco(hc.getHabitosTabaco())
                .habitosAlcohol(hc.getHabitosAlcohol())
                .habitosSustancias(hc.getHabitosSustancias())
                .habitosDetalles(hc.getHabitosDetalles())
                .atenciones(atencionesDtos)
                .ultimaConsultaSoap(ultimaSoap)
                .build();
    }

    /** Construye el DTO de la última consulta SOAP del paciente (entidad Consulta) con órdenes y profesional.
     * Prefiere la consulta más reciente que tenga al menos una orden, para que el PDF muestre las órdenes emitidas. */
    private ConsultaSoapPdfDto buildUltimaConsultaSoap(Long pacienteId) {
        List<Consulta> list = consultaRepository.findByPaciente_IdOrderByFechaConsultaDesc(
                pacienteId, PageRequest.of(0, 50));
        if (list == null || list.isEmpty()) {
            return null;
        }
        Consulta c = null;
        List<OrdenClinica> ordenes = new ArrayList<>();
        for (Consulta consulta : list) {
            List<OrdenClinica> ord = ordenClinicaRepository.findByConsulta_IdOrderByCreatedAtAsc(consulta.getId());
            if (ord != null && !ord.isEmpty()) {
                c = consulta;
                ordenes = ord;
                break;
            }
        }
        if (c == null) {
            c = list.get(0);
            ordenes = ordenClinicaRepository.findByConsulta_IdOrderByCreatedAtAsc(c.getId());
        }
        if (ordenes == null) {
            ordenes = new ArrayList<>();
        }
        if (ordenes.isEmpty()) {
            List<OrdenClinica> porPaciente = ordenClinicaRepository.findByPaciente_IdOrderByCreatedAtDesc(
                    pacienteId, PageRequest.of(0, 100));
            if (porPaciente != null) {
                List<OrdenClinica> filtradas = new ArrayList<>();
                Long consultaId = c.getId();
                for (OrdenClinica oc : porPaciente) {
                    if (oc.getConsulta() != null && consultaId.equals(oc.getConsulta().getId())) {
                        filtradas.add(oc);
                    }
                }
                ordenes = filtradas;
            }
        }
        Personal prof = c.getProfesional();

        List<OrdenPdfDto> ordenesDtos = new ArrayList<>();
        for (OrdenClinica oc : ordenes) {
            String fechaRes = oc.getFechaResultado() != null
                    ? DT_FMT.format(oc.getFechaResultado().atZone(ZoneOffset.UTC)) : null;
            ordenesDtos.add(OrdenPdfDto.builder()
                    .idOrden(oc.getId())
                    .tipo(oc.getTipo())
                    .detalle(oc.getDetalle())
                    .cantidadPrescrita(oc.getCantidadPrescrita())
                    .unidadMedida(oc.getUnidadMedida())
                    .frecuencia(oc.getFrecuencia())
                    .duracionDias(oc.getDuracionDias())
                    .estado(oc.getEstado() != null ? oc.getEstado() : "PENDIENTE")
                    .resultado(oc.getResultado())
                    .fechaResultado(fechaRes)
                    .resultadoItems(parseResultadoItems(oc.getResultado()))
                    .build());
        }

        String firmaB64 = null;
        String firmaCt = null;
        if (prof != null && prof.getFirmaData() != null) {
            firmaB64 = Base64.getEncoder().encodeToString(prof.getFirmaData());
            firmaCt = prof.getFirmaContentType();
        }

        String profNombre = prof != null
                ? (prof.getNombres() + " " + (prof.getApellidos() != null ? prof.getApellidos() : "")).trim()
                : null;

        return ConsultaSoapPdfDto.builder()
                .fechaConsulta(c.getFechaConsulta() != null
                        ? DT_FMT.format(c.getFechaConsulta().atZone(ZoneOffset.UTC)) : null)
                .motivoConsulta(c.getMotivoConsulta())
                .enfermedadActual(c.getEnfermedadActual())
                .codigoCie10(c.getCodigoCie10())
                .codigoCie10Secundario(c.getCodigoCie10Secundario())
                .presionArterial(c.getPresionArterial())
                .frecuenciaCardiaca(c.getFrecuenciaCardiaca())
                .frecuenciaRespiratoria(c.getFrecuenciaRespiratoria())
                .temperatura(c.getTemperatura())
                .peso(c.getPeso())
                .talla(c.getTalla())
                .imc(c.getImc())
                .saturacionO2(c.getSaturacionO2())
                .dolorEva(c.getDolorEva())
                .perimetroAbdominal(c.getPerimetroAbdominal())
                .perimetroCefalico(c.getPerimetroCefalico())
                .hallazgosExamen(c.getHallazgosExamen())
                .diagnostico(c.getDiagnostico())
                .planTratamiento(c.getPlanTratamiento())
                .tratamientoFarmacologico(c.getTratamientoFarmacologico())
                .observacionesClinicas(c.getObservacionesClincias())
                .recomendaciones(c.getRecomendaciones())
                .profesionalNombre(profNombre)
                .profesionalRol(prof != null ? prof.getRol() : null)
                .profesionalEspecialidad(prof != null ? prof.getEspecialidadFormal() : null)
                .profesionalIdentificacion(prof != null ? prof.getIdentificacion() : null)
                .profesionalTarjetaProfesional(prof != null ? prof.getTarjetaProfesional() : null)
                .profesionalNumeroRethus(prof != null ? prof.getNumeroRethus() : null)
                .firmaBase64(firmaB64)
                .firmaContentType(firmaCt)
                .ordenes(ordenesDtos)
                .build();
    }

    private AtencionPdfDto toAtencionDto(Atencion a) {
        Personal prof = a.getProfesional();

        List<DiagnosticoPdfDto> diagnosticos = a.getDiagnosticos().stream()
                .map(d -> DiagnosticoPdfDto.builder()
                        .codigoCie10(d.getCodigoCie10())
                        .descripcion(d.getDescripcion())
                        .tipo(d.getTipo())
                        .build())
                .collect(Collectors.toList());

        List<FormulaMedicaPdfDto> formulas = a.getFormulasMedicas().stream()
                .map(f -> FormulaMedicaPdfDto.builder()
                        .medicamento(f.getMedicamento())
                        .dosis(f.getDosis())
                        .frecuencia(f.getFrecuencia())
                        .duracion(f.getDuracion())
                        .build())
                .collect(Collectors.toList());

        List<LaboratorioPdfDto> labs = a.getLaboratorios().stream()
                .map(l -> LaboratorioPdfDto.builder()
                        .tipoExamen(l.getTipoExamen())
                        .resultado(l.getResultado())
                        .fechaResultado(l.getFechaResultado() != null
                                ? DATE_FMT.format(l.getFechaResultado().atZone(ZoneOffset.UTC)) : null)
                        .build())
                .collect(Collectors.toList());

        return AtencionPdfDto.builder()
                .id(a.getId())
                .fechaAtencion(a.getFechaAtencion() != null
                        ? DT_FMT.format(a.getFechaAtencion().atZone(ZoneOffset.UTC)) : null)
                .profesionalNombre(prof != null
                        ? (prof.getNombres() + " " + (prof.getApellidos() != null ? prof.getApellidos() : "")).trim()
                        : null)
                .profesionalRol(prof != null ? prof.getRol() : null)
                .profesionalIdentificacion(prof != null ? prof.getIdentificacion() : null)
                .profesionalEspecialidad(prof != null ? prof.getEspecialidadFormal() : null)
                .profesionalTarjetaProfesional(prof != null ? prof.getTarjetaProfesional() : null)
                .profesionalNumeroRethus(prof != null ? prof.getNumeroRethus() : null)
                .firmaBase64(prof != null && prof.getFirmaData() != null
                        ? Base64.getEncoder().encodeToString(prof.getFirmaData()) : null)
                .firmaContentType(prof != null ? prof.getFirmaContentType() : null)
                .motivoConsulta(a.getMotivoConsulta())
                .versionEnfermedad(a.getVersionEnfermedad())
                .enfermedadActual(a.getEnfermedadActual())
                .sintomasAsociados(a.getSintomasAsociados())
                .factoresMejoran(a.getFactoresMejoran())
                .factoresEmpeoran(a.getFactoresEmpeoran())
                .revisionSistemas(a.getRevisionSistemas())
                .presionArterial(a.getPresionArterial())
                .frecuenciaCardiaca(a.getFrecuenciaCardiaca())
                .frecuenciaRespiratoria(a.getFrecuenciaRespiratoria())
                .temperatura(a.getTemperatura())
                .peso(a.getPeso())
                .talla(a.getTalla())
                .imc(a.getImc())
                .evaluacionGeneral(a.getEvaluacionGeneral())
                .hallazgos(a.getHallazgos())
                .diagnosticos(diagnosticos)
                .diagnosticoTexto(a.getDiagnostico())
                .codigoCie10(a.getCodigoCie10())
                .planTratamiento(a.getPlanTratamiento())
                .tratamientoFarmacologico(a.getTratamientoFarmacologico())
                .ordenesMedicas(a.getOrdenesMedicas())
                .examenesSolicitados(a.getExamenesSolicitados())
                .incapacidad(a.getIncapacidad())
                .recomendaciones(a.getRecomendaciones())
                .formulasMedicas(formulas)
                .laboratorios(labs)
                .build();
    }

    /* ── Thymeleaf standalone render ────────────────────────────────── */

    private String renderTemplate(HistoriaClinicaPdfDto dto) {
        Context ctx = new Context();
        ctx.setVariable("data", dto);
        ctx.setVariable("ultimaConsultaSoap", dto != null ? dto.getUltimaConsultaSoap() : null);
        return TEMPLATE_ENGINE.process("pdf/historia-clinica", ctx);
    }

    /* ── HTML → PDF ─────────────────────────────────────────────────── */

    private byte[] htmlToPdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF: " + e.getMessage(), e);
        }
    }

    private EmpresaDto loadEmpresa() {
        try {
            return empresaService.findBySchemaName(TenantContextHolder.getTenantSchema()).orElse(null);
        } catch (Exception e) {
            log.debug("No se pudo cargar empresa para PDF: {}", e.getMessage());
            return null;
        }
    }
}
