/**
 * Servicio de generación de PDF premium para Historia Clínica
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sesa.salud.dto.EmpresaDto;
import com.sesa.salud.dto.pdf.*;
import com.sesa.salud.entity.*;
import com.sesa.salud.repository.HistoriaClinicaRepository;
import com.sesa.salud.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoriaClinicaPdfService {

    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final EmpresaService empresaService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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
                .pacienteTipoDocumento(p.getTipoDocumento())
                .pacienteFechaNacimiento(p.getFechaNacimiento() != null
                        ? DATE_FMT.format(p.getFechaNacimiento()) : null)
                .pacienteEdad(edad)
                .pacienteSexo(p.getSexo())
                .pacienteTelefono(p.getTelefono())
                .pacienteEmail(p.getEmail())
                .pacienteDireccion(p.getDireccion())
                .epsNombre(p.getEps() != null ? p.getEps().getNombre() : null)
                .epsCodigo(p.getEps() != null ? p.getEps().getCodigo() : null)
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
                .firmaBase64(prof != null && prof.getFirmaData() != null
                        ? Base64.getEncoder().encodeToString(prof.getFirmaData()) : null)
                .firmaContentType(prof != null ? prof.getFirmaContentType() : null)
                .motivoConsulta(a.getMotivoConsulta())
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
