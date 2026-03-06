/**
 * Implementación del servicio EBS (territorios, hogares, visitas domiciliarias).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.*;
import com.sesa.salud.entity.EbsBrigade;
import com.sesa.salud.entity.EbsBrigadeTeam;
import com.sesa.salud.entity.EbsTerritoryTeam;
import com.sesa.salud.entity.EbsAlert;
import com.sesa.salud.entity.EbsHomeVisit;
import com.sesa.salud.entity.EbsHousehold;
import com.sesa.salud.entity.EbsTerritory;
import com.sesa.salud.entity.Personal;
import com.sesa.salud.repository.EbsAlertRepository;
import com.sesa.salud.repository.EbsBrigadeRepository;
import com.sesa.salud.repository.EbsBrigadeTeamRepository;
import com.sesa.salud.repository.EbsHomeVisitRepository;
import com.sesa.salud.repository.EbsHouseholdRepository;
import com.sesa.salud.repository.EbsTerritoryRepository;
import com.sesa.salud.repository.EbsTerritoryTeamRepository;
import com.sesa.salud.repository.PersonalRepository;
import com.sesa.salud.service.IgacService;
import com.sesa.salud.security.JwtPrincipal;
import com.sesa.salud.service.EbsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EbsServiceImpl implements EbsService {

    private static final Set<String> HIGH_RISK_LEVELS = Set.of("ALTO", "MUY_ALTO");

    private final EbsTerritoryRepository territoryRepository;
    private final EbsHouseholdRepository householdRepository;
    private final EbsHomeVisitRepository homeVisitRepository;
    private final EbsBrigadeRepository ebsBrigadeRepository;
    private final EbsTerritoryTeamRepository territoryTeamRepository;
    private final EbsBrigadeTeamRepository brigadeTeamRepository;
    private final EbsAlertRepository alertRepository;
    private final PersonalRepository personalRepository;
    private final IgacService igacService;

    @Override
    @Transactional(readOnly = true)
    public List<EbsTerritorySummaryDto> listTerritories(String riskLevel) {
        List<EbsTerritory> territories = territoryRepository.findByActiveTrueOrderByName();
        List<EbsTerritorySummaryDto> result = new ArrayList<>();
        for (EbsTerritory t : territories) {
            long householdsCount = householdRepository.countByTerritoryId(t.getId());
            long visitedHouseholds = homeVisitRepository.countDistinctHouseholdsWithVisitsByTerritoryId(t.getId());
            long highRisk = HIGH_RISK_LEVELS.stream()
                .mapToLong(level -> householdRepository.countByTerritoryIdAndRiskLevel(t.getId(), level))
                .sum();
            if (riskLevel != null && !riskLevel.isEmpty() && !"TODOS".equalsIgnoreCase(riskLevel)) {
                long withLevel = householdRepository.countByTerritoryIdAndRiskLevel(t.getId(), riskLevel);
                if (withLevel == 0) continue;
            }
            String depNombre = t.getIgacDepartamentoCodigo() != null ? igacService.getDepartamentoByCodigo(t.getIgacDepartamentoCodigo()).map(d -> d.getNombre()).orElse(null) : null;
            String munNombre = t.getIgacMunicipioCodigo() != null ? igacService.getMunicipioByCodigo(t.getIgacMunicipioCodigo()).map(m -> m.getNombre()).orElse(null) : null;
            String verNombre = t.getIgacVeredaCodigo() != null ? igacService.getVeredaByCodigo(t.getIgacVeredaCodigo()).map(v -> v.getNombre()).orElse(null) : null;
            result.add(EbsTerritorySummaryDto.builder()
                .id(t.getId())
                .code(t.getCode())
                .name(t.getName())
                .type(t.getType() != null ? t.getType() : "")
                .householdsCount(householdsCount)
                .visitedHouseholdsCount(visitedHouseholds)
                .highRiskHouseholdsCount(highRisk)
                .igacDepartamentoCodigo(t.getIgacDepartamentoCodigo())
                .igacMunicipioCodigo(t.getIgacMunicipioCodigo())
                .igacVeredaCodigo(t.getIgacVeredaCodigo())
                .igacDepartamentoNombre(depNombre)
                .igacMunicipioNombre(munNombre)
                .igacVeredaNombre(verNombre)
                .build());
        }
        return result;
    }

    @Override
    @Transactional
    public EbsTerritorySummaryDto createTerritory(EbsTerritoryCreateDto dto) {
        if (territoryRepository.findByCode(dto.getCode()).isPresent()) {
            throw new RuntimeException("Ya existe un territorio con código: " + dto.getCode());
        }
        EbsTerritory t = EbsTerritory.builder()
            .code(dto.getCode())
            .name(dto.getName())
            .type(dto.getType())
            .igacDepartamentoCodigo(dto.getIgacDepartamentoCodigo())
            .igacMunicipioCodigo(dto.getIgacMunicipioCodigo())
            .igacVeredaCodigo(dto.getIgacVeredaCodigo())
            .active(true)
            .build();
        t = territoryRepository.save(t);
        String depNombre = t.getIgacDepartamentoCodigo() != null ? igacService.getDepartamentoByCodigo(t.getIgacDepartamentoCodigo()).map(d -> d.getNombre()).orElse(null) : null;
        String munNombre = t.getIgacMunicipioCodigo() != null ? igacService.getMunicipioByCodigo(t.getIgacMunicipioCodigo()).map(m -> m.getNombre()).orElse(null) : null;
        String verNombre = t.getIgacVeredaCodigo() != null ? igacService.getVeredaByCodigo(t.getIgacVeredaCodigo()).map(v -> v.getNombre()).orElse(null) : null;
        return EbsTerritorySummaryDto.builder()
            .id(t.getId())
            .code(t.getCode())
            .name(t.getName())
            .type(t.getType() != null ? t.getType() : "")
            .householdsCount(0L)
            .visitedHouseholdsCount(0L)
            .highRiskHouseholdsCount(0L)
            .igacDepartamentoCodigo(t.getIgacDepartamentoCodigo())
            .igacMunicipioCodigo(t.getIgacMunicipioCodigo())
            .igacVeredaCodigo(t.getIgacVeredaCodigo())
            .igacDepartamentoNombre(depNombre)
            .igacMunicipioNombre(munNombre)
            .igacVeredaNombre(verNombre)
            .build();
    }

    @Override
    @Transactional
    public void updateTerritoryIgac(Long territoryId, EbsTerritoryIgacUpdateDto dto) {
        EbsTerritory t = territoryRepository.findById(territoryId)
            .orElseThrow(() -> new RuntimeException("Territorio no encontrado: " + territoryId));
        if (dto.getIgacDepartamentoCodigo() != null) t.setIgacDepartamentoCodigo(dto.getIgacDepartamentoCodigo());
        if (dto.getIgacMunicipioCodigo() != null) t.setIgacMunicipioCodigo(dto.getIgacMunicipioCodigo());
        if (dto.getIgacVeredaCodigo() != null) t.setIgacVeredaCodigo(dto.getIgacVeredaCodigo());
        territoryRepository.save(t);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getTerritoryTeam(Long territoryId) {
        return territoryTeamRepository.findByTerritory_Id(territoryId).stream()
            .map(tt -> tt.getPersonal().getId()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void setTerritoryTeam(Long territoryId, List<Long> personalIds) {
        EbsTerritory territory = territoryRepository.findById(territoryId)
            .orElseThrow(() -> new RuntimeException("Territorio no encontrado: " + territoryId));
        territoryTeamRepository.deleteByTerritory_Id(territoryId);
        if (personalIds != null) {
            for (Long pid : personalIds) {
                personalRepository.findById(pid).ifPresent(p -> territoryTeamRepository.save(EbsTerritoryTeam.builder().territory(territory).personal(p).build()));
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EbsHouseholdSummaryDto> listHouseholds(Long territoryId, String riskLevel, String visitStatus) {
        String rl = (riskLevel == null || riskLevel.isEmpty() || "TODOS".equalsIgnoreCase(riskLevel)) ? null : riskLevel;
        String vs = (visitStatus == null || visitStatus.isEmpty() || "TODOS".equalsIgnoreCase(visitStatus)) ? null : visitStatus;
        List<EbsHousehold> households = householdRepository.findByTerritoryIdAndFilters(territoryId, rl, vs);
        List<EbsHouseholdSummaryDto> result = new ArrayList<>();
        for (EbsHousehold h : households) {
            Optional<Instant> lastVisit = homeVisitRepository.findLatestVisitDateByHouseholdId(h.getId());
            result.add(EbsHouseholdSummaryDto.builder()
                .id(h.getId())
                .territoryId(h.getTerritory().getId())
                .addressText(h.getAddressText() != null ? h.getAddressText() : "")
                .latitude(h.getLatitude())
                .longitude(h.getLongitude())
                .lastVisitDate(lastVisit.orElse(null))
                .riskLevel(h.getRiskLevel())
                .state(h.getState())
                .build());
        }
        return result;
    }

    @Override
    @Transactional
    public Long createHomeVisit(EbsHomeVisitRequestDto dto) {
        EbsHousehold household = householdRepository.findById(dto.getHouseholdId())
            .orElseThrow(() -> new RuntimeException("Hogar no encontrado: " + dto.getHouseholdId()));
        Instant visitDate;
        try {
            visitDate = Instant.parse(dto.getVisitDate());
        } catch (Exception e) {
            visitDate = Instant.now();
        }
        Personal professional = null;
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof JwtPrincipal principal) {
                professional = personalRepository.findByUsuario_Id(principal.userId()).orElse(null);
            }
        } catch (Exception ignored) {}
        EbsBrigade brigade = null;
        if (dto.getBrigadeId() != null) {
            brigade = ebsBrigadeRepository.findById(dto.getBrigadeId()).orElse(null);
        }
        EbsHomeVisit visit = EbsHomeVisit.builder()
            .household(household)
            .visitDate(visitDate)
            .visitType(dto.getVisitType() != null ? dto.getVisitType() : "DOMICILIARIA_APS")
            .tipoIntervencion(dto.getTipoIntervencion())
            .veredaCodigo(dto.getVeredaCodigo())
            .diagnosticoCie10(dto.getDiagnosticoCie10())
            .planCuidado(dto.getPlanCuidado())
            .brigade(brigade)
            .motivo(dto.getMotivo())
            .notes(dto.getNotes())
            .professional(professional)
            .status("COMPLETADA")
            .syncStatus("SYNCED")
            .build();
        visit = homeVisitRepository.save(visit);
        return visit.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EbsHomeVisitSummaryDto> listHomeVisits(Long territoryId, Long professionalId, Instant dateFrom, Instant dateTo) {
        List<EbsHomeVisit> visits;
        if (territoryId != null) {
            visits = homeVisitRepository.findByTerritoryIdOrderByVisitDateDesc(territoryId);
        } else if (professionalId != null) {
            visits = homeVisitRepository.findByProfessionalIdOrderByVisitDateDesc(professionalId);
        } else {
            visits = new ArrayList<>();
            for (EbsTerritory t : territoryRepository.findByActiveTrueOrderByName()) {
                visits.addAll(homeVisitRepository.findByTerritoryIdOrderByVisitDateDesc(t.getId()));
            }
            visits.sort((a, b) -> b.getVisitDate().compareTo(a.getVisitDate()));
        }
        List<EbsHomeVisitSummaryDto> result = new ArrayList<>();
        for (EbsHomeVisit v : visits) {
            if (dateFrom != null && v.getVisitDate().isBefore(dateFrom)) continue;
            if (dateTo != null && !v.getVisitDate().isBefore(dateTo)) continue;
            String profName = v.getProfessional() != null
                ? (v.getProfessional().getNombres() + " " + (v.getProfessional().getApellidos() != null ? v.getProfessional().getApellidos() : "")).trim()
                : null;
            result.add(EbsHomeVisitSummaryDto.builder()
                .id(v.getId())
                .householdId(v.getHousehold().getId())
                .householdAddress(v.getHousehold().getAddressText())
                .territoryId(v.getHousehold().getTerritory().getId())
                .territoryName(v.getHousehold().getTerritory().getName())
                .professionalId(v.getProfessional() != null ? v.getProfessional().getId() : null)
                .professionalName(profName)
                .visitDate(v.getVisitDate())
                .visitType(v.getVisitType())
                .motivo(v.getMotivo())
                .notes(v.getNotes())
                .status(v.getStatus())
                .build());
        }
        return result;
    }

    private static EbsDashboardDto emptyDashboard() {
        return EbsDashboardDto.builder()
            .totalTerritorios(0L)
            .totalHogares(0L)
            .hogaresVisitados(0L)
            .porcentajeCobertura(0.0)
            .hogaresAltoRiesgo(0L)
            .visitasEnPeriodo(0L)
            .cronicosControlados(0)
            .alertasSeguimiento(0)
            .porTerritorio(List.of())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EbsDashboardDto getDashboard(Integer diasPeriodo) {
        try {
            int days = diasPeriodo != null && diasPeriodo > 0 ? diasPeriodo : 30;
            Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
            Instant to = Instant.now().plus(1, ChronoUnit.DAYS);

            List<EbsTerritory> territories = territoryRepository.findByActiveTrueOrderByName();
            long totalTerritorios = territories.size();
            long totalHogares = 0;
            long hogaresVisitados = 0;
            long hogaresAltoRiesgo = 0;
            long visitasEnPeriodo = homeVisitRepository.countByVisitDateBetween(from, to);

            List<EbsTerritoryIndicatorDto> porTerritorio = new ArrayList<>();
            for (EbsTerritory t : territories) {
                long h = householdRepository.countByTerritoryId(t.getId());
                long v = homeVisitRepository.countDistinctHouseholdsWithVisitsByTerritoryId(t.getId());
                long altoRiesgo = HIGH_RISK_LEVELS.stream()
                    .mapToLong(level -> householdRepository.countByTerritoryIdAndRiskLevel(t.getId(), level))
                    .sum();
                totalHogares += h;
                hogaresVisitados += v;
                hogaresAltoRiesgo += altoRiesgo;
                double pct = h > 0 ? (100.0 * v / h) : 0;
                porTerritorio.add(EbsTerritoryIndicatorDto.builder()
                    .territoryId(t.getId())
                    .territoryName(t.getName())
                    .territoryCode(t.getCode())
                    .totalHogares(h)
                    .hogaresVisitados(v)
                    .porcentajeCobertura(Math.round(pct * 10) / 10.0)
                    .hogaresAltoRiesgo(altoRiesgo)
                    .visitasEnPeriodo(0)
                    .build());
            }
            double porcentajeCobertura = totalHogares > 0 ? (100.0 * hogaresVisitados / totalHogares) : 0;

            return EbsDashboardDto.builder()
                .totalTerritorios(totalTerritorios)
                .totalHogares(totalHogares)
                .hogaresVisitados(hogaresVisitados)
                .porcentajeCobertura(Math.round(porcentajeCobertura * 10) / 10.0)
                .hogaresAltoRiesgo(hogaresAltoRiesgo)
                .visitasEnPeriodo(visitasEnPeriodo)
                .cronicosControlados(0)
                .alertasSeguimiento(0)
                .porTerritorio(porTerritorio)
                .build();
        } catch (Exception e) {
            log.warn("Error al cargar dashboard EBS (se devuelve vacío): {}", e.getMessage());
            return emptyDashboard();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EbsBrigadeDto> listBrigades(Long territoryId) {
        List<EbsBrigade> list = territoryId != null
            ? ebsBrigadeRepository.findByTerritoryIdOrderByDateStartDesc(territoryId)
            : ebsBrigadeRepository.findAllByOrderByDateStartDesc();
        List<EbsBrigadeDto> result = new ArrayList<>();
        for (EbsBrigade b : list) {
            List<EbsBrigadeTeam> team = brigadeTeamRepository.findByBrigade_Id(b.getId());
            List<Long> ids = team.stream().map(t -> t.getPersonal().getId()).collect(Collectors.toList());
            List<String> names = team.stream().map(t -> (t.getPersonal().getNombres() + " " + (t.getPersonal().getApellidos() != null ? t.getPersonal().getApellidos() : "")).trim()).collect(Collectors.toList());
            result.add(EbsBrigadeDto.builder()
                .id(b.getId())
                .name(b.getName())
                .territoryId(b.getTerritory().getId())
                .territoryName(b.getTerritory().getName())
                .dateStart(b.getDateStart())
                .dateEnd(b.getDateEnd())
                .status(b.getStatus())
                .notes(b.getNotes())
                .teamMemberIds(ids)
                .teamMemberNames(names)
                .build());
        }
        return result;
    }

    @Override
    @Transactional
    public EbsBrigadeDto createBrigade(EbsBrigadeDto dto) {
        EbsTerritory territory = territoryRepository.findById(dto.getTerritoryId())
            .orElseThrow(() -> new RuntimeException("Territorio no encontrado: " + dto.getTerritoryId()));
        EbsBrigade b = EbsBrigade.builder()
            .name(dto.getName())
            .territory(territory)
            .dateStart(dto.getDateStart() != null ? dto.getDateStart() : LocalDate.now())
            .dateEnd(dto.getDateEnd() != null ? dto.getDateEnd() : LocalDate.now())
            .status(dto.getStatus() != null ? dto.getStatus() : "PROGRAMADA")
            .notes(dto.getNotes())
            .build();
        final EbsBrigade savedBrigade = ebsBrigadeRepository.save(b);
        if (dto.getTeamMemberIds() != null && !dto.getTeamMemberIds().isEmpty()) {
            for (Long pid : dto.getTeamMemberIds()) {
                personalRepository.findById(pid).ifPresent(p -> brigadeTeamRepository.save(EbsBrigadeTeam.builder().brigade(savedBrigade).personal(p).build()));
            }
        }
        return toBrigadeDto(ebsBrigadeRepository.findById(savedBrigade.getId()).orElse(savedBrigade));
    }

    @Override
    @Transactional
    public EbsBrigadeDto updateBrigade(Long id, EbsBrigadeDto dto) {
        EbsBrigade b = ebsBrigadeRepository.findById(id).orElseThrow(() -> new RuntimeException("Brigada no encontrada: " + id));
        if (dto.getName() != null) b.setName(dto.getName());
        if (dto.getDateStart() != null) b.setDateStart(dto.getDateStart());
        if (dto.getDateEnd() != null) b.setDateEnd(dto.getDateEnd());
        if (dto.getStatus() != null) b.setStatus(dto.getStatus());
        if (dto.getNotes() != null) b.setNotes(dto.getNotes());
        ebsBrigadeRepository.save(b);
        if (dto.getTeamMemberIds() != null) {
            brigadeTeamRepository.deleteByBrigade_Id(id);
            for (Long pid : dto.getTeamMemberIds()) {
                personalRepository.findById(pid).ifPresent(p -> brigadeTeamRepository.save(EbsBrigadeTeam.builder().brigade(b).personal(p).build()));
            }
        }
        return toBrigadeDto(ebsBrigadeRepository.findById(id).orElse(b));
    }

    @Override
    @Transactional
    public void deleteBrigade(Long id) {
        ebsBrigadeRepository.deleteById(id);
    }

    private EbsBrigadeDto toBrigadeDto(EbsBrigade b) {
        List<EbsBrigadeTeam> team = brigadeTeamRepository.findByBrigade_Id(b.getId());
        return EbsBrigadeDto.builder()
            .id(b.getId())
            .name(b.getName())
            .territoryId(b.getTerritory().getId())
            .territoryName(b.getTerritory().getName())
            .dateStart(b.getDateStart())
            .dateEnd(b.getDateEnd())
            .status(b.getStatus())
            .notes(b.getNotes())
            .teamMemberIds(team.stream().map(t -> t.getPersonal().getId()).collect(Collectors.toList()))
            .teamMemberNames(team.stream().map(t -> (t.getPersonal().getNombres() + " " + (t.getPersonal().getApellidos() != null ? t.getPersonal().getApellidos() : "")).trim()).collect(Collectors.toList()))
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getBrigadeTeam(Long brigadeId) {
        return brigadeTeamRepository.findByBrigade_Id(brigadeId).stream()
            .map(bt -> bt.getPersonal().getId()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void setBrigadeTeam(Long brigadeId, List<Long> personalIds) {
        EbsBrigade brigade = ebsBrigadeRepository.findById(brigadeId).orElseThrow(() -> new RuntimeException("Brigada no encontrada: " + brigadeId));
        brigadeTeamRepository.deleteByBrigade_Id(brigadeId);
        if (personalIds != null) {
            for (Long pid : personalIds) {
                personalRepository.findById(pid).ifPresent(p -> brigadeTeamRepository.save(EbsBrigadeTeam.builder().brigade(brigade).personal(p).build()));
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EbsAlertDto> listAlerts(String status) {
        List<EbsAlert> list = (status == null || status.isEmpty()) ? alertRepository.findAllByOrderByAlertDateDesc() : alertRepository.findByStatusOrderByAlertDateDesc(status);
        return list.stream().map(a -> EbsAlertDto.builder()
            .id(a.getId())
            .type(a.getType())
            .veredaCodigo(a.getVeredaCodigo())
            .municipioCodigo(a.getMunicipioCodigo())
            .departamentoCodigo(a.getDepartamentoCodigo())
            .title(a.getTitle())
            .description(a.getDescription())
            .alertDate(a.getAlertDate())
            .status(a.getStatus())
            .externalId(a.getExternalId())
            .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EbsAlertDto createAlert(EbsAlertDto dto) {
        EbsAlert a = EbsAlert.builder()
            .type(dto.getType())
            .veredaCodigo(dto.getVeredaCodigo())
            .municipioCodigo(dto.getMunicipioCodigo())
            .departamentoCodigo(dto.getDepartamentoCodigo())
            .title(dto.getTitle())
            .description(dto.getDescription())
            .alertDate(dto.getAlertDate())
            .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVA")
            .externalId(dto.getExternalId())
            .build();
        final EbsAlert savedAlert = alertRepository.save(a);
        return EbsAlertDto.builder()
            .id(savedAlert.getId())
            .type(savedAlert.getType())
            .veredaCodigo(savedAlert.getVeredaCodigo())
            .municipioCodigo(savedAlert.getMunicipioCodigo())
            .departamentoCodigo(savedAlert.getDepartamentoCodigo())
            .title(savedAlert.getTitle())
            .description(savedAlert.getDescription())
            .alertDate(savedAlert.getAlertDate())
            .status(savedAlert.getStatus())
            .externalId(savedAlert.getExternalId())
            .build();
    }

    @Override
    @Transactional
    public EbsAlertDto updateAlertStatus(Long id, String status) {
        EbsAlert a = alertRepository.findById(id).orElseThrow(() -> new RuntimeException("Alerta no encontrada: " + id));
        a.setStatus(status);
        alertRepository.save(a);
        return EbsAlertDto.builder().id(a.getId()).type(a.getType()).title(a.getTitle()).alertDate(a.getAlertDate()).status(a.getStatus()).build();
    }

    @Override
    @Transactional(readOnly = true)
    public EbsReportDataDto getReportData(String reportType, String periodFrom, String periodTo) {
        List<EbsTerritory> territories = territoryRepository.findByActiveTrueOrderByName();
        List<EbsReportDataDto.Row> rows = new ArrayList<>();
        long totalHouseholds = 0;
        long visitedHouseholds = 0;
        for (EbsTerritory t : territories) {
            long h = householdRepository.countByTerritoryId(t.getId());
            long v = homeVisitRepository.countDistinctHouseholdsWithVisitsByTerritoryId(t.getId());
            totalHouseholds += h;
            visitedHouseholds += v;
            BigDecimal pct = h > 0 ? BigDecimal.valueOf(100.0 * v / h).setScale(1, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
            String veredaName = t.getIgacVeredaCodigo() != null ? igacService.getVeredaByCodigo(t.getIgacVeredaCodigo()).map(vr -> vr.getNombre()).orElse(null) : null;
            rows.add(new EbsReportDataDto.Row(t.getName(), veredaName, h, v, pct, HIGH_RISK_LEVELS.stream().mapToLong(level -> householdRepository.countByTerritoryIdAndRiskLevel(t.getId(), level)).sum()));
        }
        BigDecimal coveragePercent = totalHouseholds > 0 ? BigDecimal.valueOf(100.0 * visitedHouseholds / totalHouseholds).setScale(1, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
        return EbsReportDataDto.builder()
            .reportType(reportType != null ? reportType : "COBERTURA")
            .periodFrom(periodFrom)
            .periodTo(periodTo)
            .totalHouseholds(BigDecimal.valueOf(totalHouseholds))
            .visitedHouseholds(BigDecimal.valueOf(visitedHouseholds))
            .coveragePercent(coveragePercent)
            .rows(rows)
            .build();
    }
}
