/**
 * Genera los Bundles FHIR R4 tipo "document" para los RDA
 * según la Guía de Implementación del Ministerio de Salud Colombia
 * Resolución 1888 de 2025 — Guía RDA CO v0.7.2
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.sesa.salud.dto.EmpresaDto;
import com.sesa.salud.entity.*;
import com.sesa.salud.service.EmpresaService;
import com.sesa.salud.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Construye un FHIR Bundle de tipo "document" que representa
 * un Resumen Digital de Atención en Salud (RDA).
 *
 * Estructura del Bundle:
 *  [0] Composition  — cabecera del documento RDA
 *  [1] Patient      — datos del paciente
 *  [2] Practitioner — profesional de salud
 *  [3] Organization — IPS / prestador
 *  [4] Encounter    — encuentro clínico
 *  [N] Condition    — diagnósticos CIE-10
 *  [N] Observation  — signos vitales
 *  [N] MedicationStatement — medicamentos formulados
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RdaGeneratorService {

    private final FhirMapperService mapper;
    private final EmpresaService    empresaService;

    // FHIR Context singleton (thread-safe después de inicialización)
    private static final FhirContext FHIR_CTX = FhirContext.forR4();

    // Canonical base de los perfiles Colombia
    private static final String PROFILE_RDA_CONSULTA =
            "https://fhir.minsalud.gov.co/rda/StructureDefinition/RDA-ConsultaExterna";
    private static final String PROFILE_RDA_PACIENTE  =
            "https://fhir.minsalud.gov.co/rda/StructureDefinition/RDA-Paciente";

    // LOINC code para documento de consulta ambulatoria
    private static final String LOINC_CONSULTA_EXTERNA = "11488-4";

    // ═══════════════════════════════════════════════════════════════════════
    //  GENERACIÓN PRINCIPAL
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Genera el Bundle RDA de Consulta Externa para una Atención.
     * Este es el tipo principal de RDA que genera SESA.
     */
    @Transactional(readOnly = true)
    public String generarRdaConsultaExterna(Atencion atencion) {
        Bundle bundle = buildBundle(atencion, RdaEnvio.TipoRda.CONSULTA_EXTERNA);
        return serializarJson(bundle);
    }

    /**
     * Genera el Bundle RDA de Paciente (resumen global de la HC).
     */
    @Transactional(readOnly = true)
    public String generarRdaPaciente(Atencion atencion) {
        Bundle bundle = buildBundle(atencion, RdaEnvio.TipoRda.PACIENTE);
        return serializarJson(bundle);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CONSTRUCCIÓN DEL BUNDLE
    // ═══════════════════════════════════════════════════════════════════════

    private Bundle buildBundle(Atencion atencion, RdaEnvio.TipoRda tipo) {
        String bundleId = UUID.randomUUID().toString();
        Date ahora = new Date();

        // ── 1. Recursos base ───────────────────────────────────────────────
        Paciente paciente    = atencion.getHistoriaClinica().getPaciente();
        Personal profesional = atencion.getProfesional();
        EmpresaDto empresa   = cargarEmpresa();

        Patient      fhirPatient  = mapper.mapPaciente(paciente);
        Practitioner fhirPractit  = mapper.mapProfesional(profesional);
        Organization fhirOrg      = mapper.mapOrganizacion(empresa);

        Reference patientRef = ref(fhirPatient);
        Reference practRef   = ref(fhirPractit);
        Reference orgRef     = ref(fhirOrg);

        Date fechaAtencion = atencion.getFechaAtencion() != null
                ? Date.from(atencion.getFechaAtencion()) : ahora;

        Encounter fhirEncounter = mapper.mapEncuentro(atencion, patientRef, practRef, orgRef);
        Reference encounterRef  = ref(fhirEncounter);

        // ── 2. Diagnósticos ────────────────────────────────────────────────
        List<Condition> conditions = atencion.getDiagnosticos().stream()
                .map(d -> mapper.mapDiagnostico(d, patientRef, encounterRef))
                .toList();

        // ── 3. Signos vitales ──────────────────────────────────────────────
        List<Observation> observations = buildSignosVitales(
                atencion, patientRef, encounterRef, fechaAtencion);

        // ── 4. Medicamentos ────────────────────────────────────────────────
        List<MedicationStatement> meds = atencion.getFormulasMedicas().stream()
                .map(fm -> mapper.mapMedicamento(fm, patientRef, encounterRef))
                .toList();

        // ── 5. Composition ─────────────────────────────────────────────────
        Composition composition = buildComposition(
                tipo, atencion, fhirPatient, fhirPractit, fhirOrg,
                fhirEncounter, conditions, observations, meds, ahora);

        // ── 6. Ensamblar Bundle ────────────────────────────────────────────
        Bundle bundle = new Bundle();
        bundle.setId(bundleId);
        bundle.setType(Bundle.BundleType.DOCUMENT);
        bundle.setTimestamp(ahora);

        bundle.getMeta()
              .addProfile(tipo == RdaEnvio.TipoRda.CONSULTA_EXTERNA
                      ? PROFILE_RDA_CONSULTA : PROFILE_RDA_PACIENTE);

        // Identifier del bundle (requerido por la guía Colombia)
        bundle.setIdentifier(new Identifier()
                .setSystem("https://fhir.minsalud.gov.co/rda/bundle-id")
                .setValue(bundleId));

        // Orden requerida: Composition primero
        addEntry(bundle, composition);
        addEntry(bundle, fhirPatient);
        addEntry(bundle, fhirPractit);
        addEntry(bundle, fhirOrg);
        addEntry(bundle, fhirEncounter);
        conditions.forEach(c  -> addEntry(bundle, c));
        observations.forEach(o -> addEntry(bundle, o));
        meds.forEach(m         -> addEntry(bundle, m));

        log.info("Bundle RDA generado — tipo:{} bundleId:{} atencionId:{}",
                tipo, bundleId, atencion.getId());
        return bundle;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  COMPOSITION (cabecera del documento RDA)
    // ═══════════════════════════════════════════════════════════════════════

    private Composition buildComposition(RdaEnvio.TipoRda tipo,
                                          Atencion atencion,
                                          Patient patient,
                                          Practitioner practitioner,
                                          Organization org,
                                          Encounter encounter,
                                          List<Condition> conditions,
                                          List<Observation> observations,
                                          List<MedicationStatement> meds,
                                          Date ahora) {
        Composition comp = new Composition();
        comp.setId(UUID.randomUUID().toString());
        comp.setStatus(Composition.CompositionStatus.FINAL);
        comp.setDate(ahora);

        // Tipo de documento LOINC
        comp.getType()
            .addCoding(new Coding()
                    .setSystem("http://loinc.org")
                    .setCode(LOINC_CONSULTA_EXTERNA)
                    .setDisplay("Consult note"))
            .setText("Resumen Digital de Atención en Salud");

        // Título
        comp.setTitle("RDA - " + (tipo == RdaEnvio.TipoRda.CONSULTA_EXTERNA
                ? "Consulta Externa" : "Resumen de Paciente"));

        // Referencias obligatorias
        comp.setSubject(ref(patient));
        comp.setEncounter(ref(encounter));
        comp.addAuthor(ref(practitioner));
        comp.setCustodian(ref(org));

        // Identificador del documento
        comp.setIdentifier(new Identifier()
                .setSystem("https://fhir.minsalud.gov.co/rda/composition-id")
                .setValue(UUID.randomUUID().toString()));

        // ── Secciones del RDA ──────────────────────────────────────────────

        // Sección: Motivo de consulta
        if (atencion.getMotivoConsulta() != null) {
            comp.addSection()
                .setTitle("Motivo de Consulta")
                .getCode().addCoding(new Coding()
                        .setSystem("http://loinc.org")
                        .setCode("46239-0")
                        .setDisplay("Chief complaint"));
        }

        // Sección: Diagnósticos
        if (!conditions.isEmpty()) {
            Composition.SectionComponent secDiag = comp.addSection();
            secDiag.setTitle("Diagnósticos");
            secDiag.getCode().addCoding(new Coding()
                    .setSystem("http://loinc.org")
                    .setCode("11450-4")
                    .setDisplay("Problem list"));
            conditions.forEach(c -> secDiag.addEntry(ref(c)));
        }

        // Sección: Signos vitales
        if (!observations.isEmpty()) {
            Composition.SectionComponent secVit = comp.addSection();
            secVit.setTitle("Signos Vitales");
            secVit.getCode().addCoding(new Coding()
                    .setSystem("http://loinc.org")
                    .setCode("8716-3")
                    .setDisplay("Vital signs"));
            observations.forEach(o -> secVit.addEntry(ref(o)));
        }

        // Sección: Medicamentos
        if (!meds.isEmpty()) {
            Composition.SectionComponent secMed = comp.addSection();
            secMed.setTitle("Fórmula Médica");
            secMed.getCode().addCoding(new Coding()
                    .setSystem("http://loinc.org")
                    .setCode("10160-0")
                    .setDisplay("History of Medication use"));
            meds.forEach(m -> secMed.addEntry(ref(m)));
        }

        // Sección: Plan de tratamiento
        if (atencion.getPlanTratamiento() != null) {
            Composition.SectionComponent secPlan = comp.addSection();
            secPlan.setTitle("Plan de Tratamiento");
            secPlan.getCode().addCoding(new Coding()
                    .setSystem("http://loinc.org")
                    .setCode("18776-5")
                    .setDisplay("Plan of care note"));
        }

        return comp;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SIGNOS VITALES
    // ═══════════════════════════════════════════════════════════════════════

    private List<Observation> buildSignosVitales(Atencion a,
                                                  Reference patRef,
                                                  Reference encRef,
                                                  Date fecha) {
        java.util.List<Observation> list = new java.util.ArrayList<>();

        if (a.getPresionArterial() != null) {
            list.add(mapper.mapPresionArterial(a.getPresionArterial(), patRef, encRef, fecha));
        }
        if (a.getFrecuenciaCardiaca() != null) {
            list.add(mapper.mapSignoVital(mapper.getLoincFc(),
                    "Heart rate", a.getFrecuenciaCardiaca(), "/min", patRef, encRef, fecha));
        }
        if (a.getFrecuenciaRespiratoria() != null) {
            list.add(mapper.mapSignoVital(mapper.getLoincFr(),
                    "Respiratory rate", a.getFrecuenciaRespiratoria(), "/min", patRef, encRef, fecha));
        }
        if (a.getTemperatura() != null) {
            list.add(mapper.mapSignoVital(mapper.getLoincTemp(),
                    "Body temperature", a.getTemperatura(), "Cel", patRef, encRef, fecha));
        }
        if (a.getPeso() != null) {
            list.add(mapper.mapSignoVital(mapper.getLoincPeso(),
                    "Body weight", a.getPeso(), "kg", patRef, encRef, fecha));
        }
        if (a.getTalla() != null) {
            list.add(mapper.mapSignoVital(mapper.getLoincTalla(),
                    "Body height", a.getTalla(), "cm", patRef, encRef, fecha));
        }
        if (a.getImc() != null) {
            list.add(mapper.mapSignoVital(mapper.getLoincImc(),
                    "BMI", a.getImc(), "kg/m2", patRef, encRef, fecha));
        }
        return list;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  UTILIDADES
    // ═══════════════════════════════════════════════════════════════════════

    private Reference ref(Resource resource) {
        Reference ref = new Reference(resource.getResourceType().name() + "/" + resource.getId());
        ref.setResource(resource);
        return ref;
    }

    private void addEntry(Bundle bundle, Resource resource) {
        bundle.addEntry()
              .setFullUrl("urn:uuid:" + resource.getId())
              .setResource(resource);
    }

    public String serializarJson(Bundle bundle) {
        IParser parser = FHIR_CTX.newJsonParser();
        parser.setPrettyPrint(false);
        return parser.encodeResourceToString(bundle);
    }

    public String serializarJsonPretty(Bundle bundle) {
        IParser parser = FHIR_CTX.newJsonParser();
        parser.setPrettyPrint(true);
        return parser.encodeResourceToString(bundle);
    }

    private EmpresaDto cargarEmpresa() {
        try {
            return empresaService
                    .findBySchemaName(TenantContextHolder.getTenantSchema())
                    .orElse(fallbackEmpresa());
        } catch (Exception e) {
            log.warn("No se pudo cargar empresa para RDA: {}", e.getMessage());
            return fallbackEmpresa();
        }
    }

    private EmpresaDto fallbackEmpresa() {
        EmpresaDto dto = new EmpresaDto();
        dto.setRazonSocial("IPS SESA Salud");
        return dto;
    }
}
