/**
 * Mapeo de entidades SESA a recursos HL7 FHIR R4
 * Conforme a la Resolución 1888 de 2025 y Guía de Implementación RDA Colombia v0.7.2
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.fhir;

import com.sesa.salud.dto.EmpresaDto;
import com.sesa.salud.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

/**
 * Transforma las entidades del dominio SESA a los recursos FHIR R4
 * definidos en la Guía de Implementación RDA (CO) del Ministerio de Salud.
 *
 * Namespaces de sistema utilizados (Colombia):
 *  - Paciente identificador:  https://www.datos.gov.co/id/paciente
 *  - Profesional identificador: https://www.datos.gov.co/id/profesional
 *  - Organización NIT:         https://www.datos.gov.co/id/organizacion
 *  - Código CIE-10:            http://hl7.org/fhir/sid/icd-10
 *  - LOINC (signos vitales):   http://loinc.org
 *  - Encuentro clase:          http://terminology.hl7.org/CodeSystem/v3-ActCode
 */
@Slf4j
@Service
public class FhirMapperService {

    // ─── Sistemas de identificación Colombia ────────────────────────────────
    private static final String SYS_PACIENTE_ID   = "https://www.datos.gov.co/id/paciente";
    private static final String SYS_PROFESIONAL   = "https://www.datos.gov.co/id/profesional";
    private static final String SYS_ORGANIZACION  = "https://www.datos.gov.co/id/organizacion";
    private static final String SYS_CIE10         = "http://hl7.org/fhir/sid/icd-10";
    private static final String SYS_LOINC         = "http://loinc.org";
    private static final String SYS_ENCOUNTER_CLS = "http://terminology.hl7.org/CodeSystem/v3-ActCode";
    // ─── Tipos de documento Colombia (RIPS) ────────────────────────────────
    private static final String SYS_TIPO_DOC = "https://fhir.minsalud.gov.co/rda/CodeSystem/IHCE-TipoDocumento";

    // ─── LOINC codes para signos vitales ────────────────────────────────────
    private static final String LOINC_TA_SYS    = "8480-6";  // Presión sistólica
    private static final String LOINC_TA_DIA    = "8462-4";  // Presión diastólica
    private static final String LOINC_TA_PANEL  = "55284-4"; // Panel TA
    private static final String LOINC_FC        = "8867-4";  // Frecuencia cardíaca
    private static final String LOINC_FR        = "9279-1";  // Frecuencia respiratoria
    private static final String LOINC_TEMP      = "8310-5";  // Temperatura
    private static final String LOINC_PESO      = "29463-7"; // Peso
    private static final String LOINC_TALLA     = "8302-2";  // Talla
    private static final String LOINC_IMC       = "39156-5"; // IMC

    // ═══════════════════════════════════════════════════════════════════════
    //  PATIENT
    // ═══════════════════════════════════════════════════════════════════════

    public Patient mapPaciente(Paciente p) {
        Patient fhir = new Patient();
        fhir.setId(UUID.randomUUID().toString());

        // Identificador con tipo de documento
        Identifier id = fhir.addIdentifier();
        id.setSystem(SYS_PACIENTE_ID);
        id.setValue(p.getDocumento());
        if (p.getTipoDocumento() != null) {
            id.getType()
              .addCoding()
              .setSystem(SYS_TIPO_DOC)
              .setCode(normalizarTipoDoc(p.getTipoDocumento()))
              .setDisplay(p.getTipoDocumento());
        }

        // Nombre
        HumanName nombre = fhir.addName();
        nombre.setUse(HumanName.NameUse.OFFICIAL);
        nombre.setFamily(p.getApellidos() != null ? p.getApellidos() : "");
        if (p.getNombres() != null) {
            for (String n : p.getNombres().split(" ")) {
                nombre.addGiven(n);
            }
        }

        // Género
        if (p.getSexo() != null) {
            fhir.setGender(mapGenero(p.getSexo()));
        }

        // Fecha de nacimiento
        if (p.getFechaNacimiento() != null) {
            fhir.setBirthDate(java.sql.Date.valueOf(p.getFechaNacimiento()));
        }

        // Teléfono
        if (p.getTelefono() != null) {
            fhir.addTelecom()
                .setSystem(ContactPoint.ContactPointSystem.PHONE)
                .setValue(p.getTelefono())
                .setUse(ContactPoint.ContactPointUse.MOBILE);
        }

        // Dirección
        if (p.getDireccion() != null) {
            fhir.addAddress().setText(p.getDireccion()).setCountry("CO");
        }

        return fhir;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PRACTITIONER
    // ═══════════════════════════════════════════════════════════════════════

    public Practitioner mapProfesional(Personal prof) {
        Practitioner fhir = new Practitioner();
        fhir.setId(UUID.randomUUID().toString());

        fhir.addIdentifier()
            .setSystem(SYS_PROFESIONAL)
            .setValue(prof.getIdentificacion());

        HumanName nombre = fhir.addName();
        nombre.setUse(HumanName.NameUse.OFFICIAL);
        nombre.setFamily(prof.getApellidos() != null ? prof.getApellidos() : "");
        if (prof.getNombres() != null) {
            for (String n : prof.getNombres().split(" ")) {
                nombre.addGiven(n);
            }
        }

        // Rol como calificación
        if (prof.getRol() != null) {
            Practitioner.PractitionerQualificationComponent qual = fhir.addQualification();
            qual.getCode().setText(prof.getRol());
        }

        return fhir;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ORGANIZATION
    // ═══════════════════════════════════════════════════════════════════════

    public Organization mapOrganizacion(EmpresaDto empresa) {
        Organization fhir = new Organization();
        fhir.setId(UUID.randomUUID().toString());

        if (empresa.getIdentificacion() != null) {
            fhir.addIdentifier()
                .setSystem(SYS_ORGANIZACION)
                .setValue(empresa.getIdentificacion())
                .getType().addCoding()
                    .setSystem(SYS_TIPO_DOC)
                    .setCode("NIT")
                    .setDisplay("NIT");
        }

        fhir.setName(empresa.getRazonSocial());
        fhir.setActive(true);

        if (empresa.getTelefono() != null) {
            fhir.addTelecom()
                .setSystem(ContactPoint.ContactPointSystem.PHONE)
                .setValue(empresa.getTelefono());
        }

        if (empresa.getDireccionEmpresa() != null) {
            fhir.addAddress()
                .setText(empresa.getDireccionEmpresa())
                .setCountry("CO");
        }

        return fhir;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ENCOUNTER
    // ═══════════════════════════════════════════════════════════════════════

    public Encounter mapEncuentro(Atencion atencion,
                                  Reference patientRef,
                                  Reference practitionerRef,
                                  Reference orgRef) {
        Encounter fhir = new Encounter();
        fhir.setId(UUID.randomUUID().toString());
        fhir.setStatus(Encounter.EncounterStatus.FINISHED);

        // Clase del encuentro: AMB = ambulatorio (consulta externa)
        fhir.setClass_(new Coding()
                .setSystem(SYS_ENCOUNTER_CLS)
                .setCode("AMB")
                .setDisplay("ambulatory"));

        fhir.setSubject(patientRef);

        // Participante (profesional)
        if (practitionerRef != null) {
            fhir.addParticipant()
                .setIndividual(practitionerRef);
        }

        // Organización prestadora
        if (orgRef != null) {
            fhir.setServiceProvider(orgRef);
        }

        // Período de la atención
        if (atencion.getFechaAtencion() != null) {
            Date fecha = Date.from(atencion.getFechaAtencion());
            fhir.setPeriod(new Period().setStart(fecha).setEnd(fecha));
        }

        // Motivo de consulta
        if (atencion.getMotivoConsulta() != null) {
            fhir.addReasonCode()
                .setText(atencion.getMotivoConsulta());
        }

        return fhir;
    }

    /** S11: Encuentro FHIR desde registro de urgencias (clase EMER). */
    public Encounter mapEncuentroUrgencia(com.sesa.salud.entity.UrgenciaRegistro urgencia,
                                          Reference patientRef,
                                          Reference practitionerRef,
                                          Reference orgRef) {
        Encounter fhir = new Encounter();
        fhir.setId(UUID.randomUUID().toString());
        fhir.setStatus(Encounter.EncounterStatus.FINISHED);
        fhir.setClass_(new Coding()
                .setSystem(SYS_ENCOUNTER_CLS)
                .setCode("EMER")
                .setDisplay("emergency"));
        fhir.setSubject(patientRef);
        if (practitionerRef != null) {
            fhir.addParticipant().setIndividual(practitionerRef);
        }
        if (orgRef != null) {
            fhir.setServiceProvider(orgRef);
        }
        java.time.LocalDateTime ingreso = urgencia.getFechaHoraIngreso();
        if (ingreso != null) {
            Date start = java.sql.Timestamp.valueOf(ingreso);
            fhir.setPeriod(new Period().setStart(start).setEnd(start));
        }
        if (urgencia.getMotivoConsulta() != null) {
            fhir.addReasonCode().setText(urgencia.getMotivoConsulta());
        }
        return fhir;
    }

    /** S11: Encuentro FHIR desde hospitalización (clase IMP). */
    public Encounter mapEncuentroHospitalizacion(com.sesa.salud.entity.Hospitalizacion hosp,
                                                Reference patientRef,
                                                Reference orgRef) {
        Encounter fhir = new Encounter();
        fhir.setId(UUID.randomUUID().toString());
        fhir.setStatus(Encounter.EncounterStatus.FINISHED);
        fhir.setClass_(new Coding()
                .setSystem(SYS_ENCOUNTER_CLS)
                .setCode("IMP")
                .setDisplay("inpatient encounter"));
        fhir.setSubject(patientRef);
        if (orgRef != null) {
            fhir.setServiceProvider(orgRef);
        }
        java.time.LocalDateTime ingreso = hosp.getFechaIngreso();
        java.time.LocalDateTime egreso = hosp.getFechaEgreso();
        if (ingreso != null) {
            Date start = java.sql.Timestamp.valueOf(ingreso);
            Date end = egreso != null ? java.sql.Timestamp.valueOf(egreso) : start;
            fhir.setPeriod(new Period().setStart(start).setEnd(end));
        }
        if (hosp.getServicio() != null) {
            fhir.addReasonCode().setText(hosp.getServicio());
        }
        return fhir;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CONDITION (Diagnóstico CIE-10)
    // ═══════════════════════════════════════════════════════════════════════

    public Condition mapDiagnostico(Diagnostico diag,
                                    Reference patientRef,
                                    Reference encounterRef) {
        Condition fhir = new Condition();
        fhir.setId(UUID.randomUUID().toString());

        // Estado clínico
        fhir.setClinicalStatus(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                        .setCode("active")
                        .setDisplay("Active")));

        // Verificación
        fhir.setVerificationStatus(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/condition-ver-status")
                        .setCode("confirmed")
                        .setDisplay("Confirmed")));

        // Código CIE-10
        if (diag.getCodigoCie10() != null) {
            fhir.getCode()
                .addCoding(new Coding()
                        .setSystem(SYS_CIE10)
                        .setCode(diag.getCodigoCie10())
                        .setDisplay(diag.getDescripcion()));
            fhir.getCode().setText(diag.getDescripcion());
        }

        // Categoría: diagnóstico del encuentro
        fhir.addCategory()
            .addCoding(new Coding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/condition-category")
                    .setCode("encounter-diagnosis")
                    .setDisplay("Encounter Diagnosis"));

        // Tipo (principal / secundario)
        if (diag.getTipo() != null) {
            fhir.addNote().setText("Tipo: " + diag.getTipo());
        }

        fhir.setSubject(patientRef);
        fhir.setEncounter(encounterRef);

        return fhir;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  OBSERVATION — Signos Vitales
    // ═══════════════════════════════════════════════════════════════════════

    public Observation mapPresionArterial(String valor,
                                          Reference patientRef,
                                          Reference encounterRef,
                                          Date fecha) {
        Observation obs = new Observation();
        obs.setId(UUID.randomUUID().toString());
        obs.setStatus(Observation.ObservationStatus.FINAL);

        obs.addCategory()
           .addCoding(new Coding()
                   .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                   .setCode("vital-signs")
                   .setDisplay("Vital Signs"));

        obs.getCode()
           .addCoding(new Coding().setSystem(SYS_LOINC).setCode(LOINC_TA_PANEL).setDisplay("Blood pressure"))
           .setText("Presión Arterial");

        obs.setSubject(patientRef);
        obs.setEncounter(encounterRef);
        obs.setEffective(new DateTimeType(fecha));

        // Componentes sistólica / diastólica
        String[] partes = valor.split("[/\\-]");
        if (partes.length >= 2) {
            // Sistólica
            Observation.ObservationComponentComponent sistolica = obs.addComponent();
            sistolica.getCode().addCoding(new Coding().setSystem(SYS_LOINC).setCode(LOINC_TA_SYS));
            parseDecimalQuantity(partes[0].trim(), "mmHg", sistolica);

            // Diastólica
            Observation.ObservationComponentComponent diastolica = obs.addComponent();
            diastolica.getCode().addCoding(new Coding().setSystem(SYS_LOINC).setCode(LOINC_TA_DIA));
            parseDecimalQuantity(partes[1].trim(), "mmHg", diastolica);
        } else {
            obs.setValue(new StringType(valor));
        }

        return obs;
    }

    public Observation mapSignoVital(String loincCode, String displayName,
                                     String valor, String unidad,
                                     Reference patientRef, Reference encounterRef,
                                     Date fecha) {
        Observation obs = new Observation();
        obs.setId(UUID.randomUUID().toString());
        obs.setStatus(Observation.ObservationStatus.FINAL);

        obs.addCategory()
           .addCoding(new Coding()
                   .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                   .setCode("vital-signs"));

        obs.getCode()
           .addCoding(new Coding().setSystem(SYS_LOINC).setCode(loincCode).setDisplay(displayName))
           .setText(displayName);

        obs.setSubject(patientRef);
        obs.setEncounter(encounterRef);
        if (fecha != null) obs.setEffective(new DateTimeType(fecha));

        try {
            BigDecimal num = new BigDecimal(valor.replace(",", ".").trim());
            obs.setValue(new Quantity()
                    .setValue(num)
                    .setUnit(unidad)
                    .setSystem("http://unitsofmeasure.org")
                    .setCode(unidad));
        } catch (NumberFormatException e) {
            obs.setValue(new StringType(valor));
        }

        return obs;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  MEDICATION STATEMENT
    // ═══════════════════════════════════════════════════════════════════════

    public MedicationStatement mapMedicamento(FormulaMedica fm,
                                              Reference patientRef,
                                              Reference encounterRef) {
        MedicationStatement fhir = new MedicationStatement();
        fhir.setId(UUID.randomUUID().toString());
        fhir.setStatus(MedicationStatement.MedicationStatementStatus.ACTIVE);

        fhir.setMedication(new CodeableConcept().setText(fm.getMedicamento()));
        fhir.setSubject(patientRef);
        fhir.addDerivedFrom(encounterRef);

        // Dosificación
        Dosage dosage = fhir.addDosage();
        StringBuilder dosText = new StringBuilder();
        if (fm.getDosis() != null)       dosText.append("Dosis: ").append(fm.getDosis()).append(" ");
        if (fm.getFrecuencia() != null)  dosText.append("Frecuencia: ").append(fm.getFrecuencia()).append(" ");
        if (fm.getDuracion() != null)    dosText.append("Duración: ").append(fm.getDuracion());
        dosage.setText(dosText.toString().trim());

        return fhir;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private Enumerations.AdministrativeGender mapGenero(String sexo) {
        if (sexo == null) return Enumerations.AdministrativeGender.UNKNOWN;
        return switch (sexo.toUpperCase().charAt(0)) {
            case 'M' -> Enumerations.AdministrativeGender.MALE;
            case 'F' -> Enumerations.AdministrativeGender.FEMALE;
            default  -> Enumerations.AdministrativeGender.OTHER;
        };
    }

    private String normalizarTipoDoc(String tipo) {
        if (tipo == null) return "CC";
        return switch (tipo.toUpperCase().trim()) {
            case "CÉDULA DE CIUDADANÍA", "CC" -> "CC";
            case "TARJETA DE IDENTIDAD", "TI"  -> "TI";
            case "CÉDULA DE EXTRANJERÍA", "CE"  -> "CE";
            case "REGISTRO CIVIL", "RC"         -> "RC";
            case "PASAPORTE", "PA"              -> "PA";
            default -> tipo.toUpperCase();
        };
    }

    private void parseDecimalQuantity(String valor, String unit,
                                       Observation.ObservationComponentComponent comp) {
        try {
            comp.setValue(new Quantity()
                    .setValue(new BigDecimal(valor))
                    .setUnit(unit)
                    .setSystem("http://unitsofmeasure.org")
                    .setCode(unit));
        } catch (NumberFormatException e) {
            comp.setValue(new StringType(valor));
        }
    }

    // Getters para constantes LOINC (usados por RdaGeneratorService)
    public String getLoincFc()    { return LOINC_FC; }
    public String getLoincFr()    { return LOINC_FR; }
    public String getLoincTemp()  { return LOINC_TEMP; }
    public String getLoincPeso()  { return LOINC_PESO; }
    public String getLoincTalla() { return LOINC_TALLA; }
    public String getLoincImc()   { return LOINC_IMC; }
}
