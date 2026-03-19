/**
 * S5: Reconciliación de medicamentos y alergias por atención.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.ReconciliacionAtencionDto;
import com.sesa.salud.dto.ReconciliacionAtencionRequestDto;
import com.sesa.salud.entity.Atencion;
import com.sesa.salud.entity.HistoriaClinica;
import com.sesa.salud.entity.Personal;
import com.sesa.salud.entity.ReconciliacionAtencion;
import com.sesa.salud.repository.AtencionRepository;
import com.sesa.salud.repository.HistoriaClinicaRepository;
import com.sesa.salud.repository.PersonalRepository;
import com.sesa.salud.repository.ReconciliacionAtencionRepository;
import com.sesa.salud.security.JwtPrincipal;
import com.sesa.salud.service.ReconciliacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReconciliacionServiceImpl implements ReconciliacionService {

    private final ReconciliacionAtencionRepository reconciliacionRepository;
    private final AtencionRepository atencionRepository;
    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final PersonalRepository personalRepository;

    private static List<String> textToList(String s) {
        if (s == null || s.isBlank()) return new ArrayList<>();
        return Arrays.stream(s.split("[\n;]"))
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .collect(Collectors.toList());
    }

    private static String listToText(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return String.join("\n", list);
    }

    private Personal getCurrentUserPersonal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof JwtPrincipal principal)) {
            return null;
        }
        return personalRepository.findByUsuario_Id(principal.userId()).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public ReconciliacionAtencionDto getByAtencionId(Long atencionId) {
        Atencion atencion = atencionRepository.findById(atencionId)
                .orElseThrow(() -> new RuntimeException("Atención no encontrada: " + atencionId));
        HistoriaClinica hc = atencion.getHistoriaClinica();
        String medicamentosHc = hc.getAntecedentesFarmacologicos();
        String alergiasHc = hc.getAlergiasGenerales();

        return reconciliacionRepository.findByAtencion_Id(atencionId)
                .map(r -> toDto(r, textToList(r.getMedicamentosHc()), textToList(r.getAlergiasHc())))
                .orElseGet(() -> ReconciliacionAtencionDto.builder()
                        .atencionId(atencionId)
                        .medicamentosHc(textToList(medicamentosHc))
                        .alergiasHc(textToList(alergiasHc))
                        .medicamentosReferidos(new ArrayList<>())
                        .alergiasReferidas(new ArrayList<>())
                        .build());
    }

    @Override
    @Transactional
    public ReconciliacionAtencionDto guardar(Long atencionId, ReconciliacionAtencionRequestDto request) {
        Atencion atencion = atencionRepository.findById(atencionId)
                .orElseThrow(() -> new RuntimeException("Atención no encontrada: " + atencionId));
        Personal profesional = getCurrentUserPersonal();
        if (profesional == null) {
            throw new RuntimeException("No se pudo identificar al profesional. Debe estar autenticado.");
        }
        HistoriaClinica hc = atencion.getHistoriaClinica();
        String medicamentosHc = hc.getAntecedentesFarmacologicos();
        String alergiasHc = hc.getAlergiasGenerales();

        ReconciliacionAtencion r = reconciliacionRepository.findByAtencion_Id(atencionId).orElse(null);
        if (r == null) {
            r = new ReconciliacionAtencion();
            r.setAtencion(atencion);
        }
        r.setProfesional(profesional);
        r.setMedicamentosReferidos(listToText(request.getMedicamentosReferidos()));
        r.setAlergiasReferidas(listToText(request.getAlergiasReferidas()));
        r.setMedicamentosHc(medicamentosHc);
        r.setAlergiasHc(alergiasHc);
        r.setReconciliadoAt(Instant.now());
        r.setObservaciones(request.getObservaciones());
        r = reconciliacionRepository.save(r);
        return toDto(r, textToList(r.getMedicamentosHc()), textToList(r.getAlergiasHc()));
    }

    private ReconciliacionAtencionDto toDto(ReconciliacionAtencion r, List<String> medicamentosHcList, List<String> alergiasHcList) {
        Personal p = r.getProfesional();
        String nombreProfesional = (p.getNombres() != null ? p.getNombres() : "") + " " + (p.getApellidos() != null ? p.getApellidos() : "");
        return ReconciliacionAtencionDto.builder()
                .id(r.getId())
                .atencionId(r.getAtencion().getId())
                .profesionalId(p.getId())
                .nombreProfesional(nombreProfesional.trim())
                .medicamentosReferidos(textToList(r.getMedicamentosReferidos()))
                .medicamentosHc(medicamentosHcList)
                .alergiasReferidas(textToList(r.getAlergiasReferidas()))
                .alergiasHc(alergiasHcList)
                .reconciliadoAt(r.getReconciliadoAt())
                .observaciones(r.getObservaciones())
                .build();
    }
}
