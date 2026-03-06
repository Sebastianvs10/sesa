/**
 * Controlador REST del módulo EBS (Equipos Básicos de Salud).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.*;
import com.sesa.salud.service.EbsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ebs")
@RequiredArgsConstructor
public class EbsController {

    private static final String ROLES_EBS = "hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ENFERMERO','JEFE_ENFERMERIA','AUXILIAR_ENFERMERIA','EBS','COORDINADOR_TERRITORIAL','SUPERVISOR_APS')";

    private final EbsService ebsService;

    @GetMapping("/territories")
    @PreAuthorize(ROLES_EBS)
    public List<EbsTerritorySummaryDto> listTerritories(
            @RequestParam(value = "riskLevel", required = false) String riskLevel) {
        return ebsService.listTerritories(riskLevel);
    }

    @PostMapping("/territories")
    @PreAuthorize(ROLES_EBS)
    public ResponseEntity<EbsTerritorySummaryDto> createTerritory(@Valid @RequestBody EbsTerritoryCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ebsService.createTerritory(dto));
    }

    @GetMapping("/territories/{id}/team")
    @PreAuthorize(ROLES_EBS)
    public List<Long> getTerritoryTeam(@PathVariable Long id) {
        return ebsService.getTerritoryTeam(id);
    }

    @PutMapping("/territories/{id}/team")
    @PreAuthorize(ROLES_EBS)
    public ResponseEntity<Map<String, String>> setTerritoryTeam(@PathVariable Long id, @RequestBody List<Long> personalIds) {
        ebsService.setTerritoryTeam(id, personalIds);
        return ResponseEntity.ok(Map.of("message", "Equipo actualizado"));
    }

    @PutMapping("/territories/{id}/igac")
    @PreAuthorize(ROLES_EBS)
    public ResponseEntity<Map<String, String>> updateTerritoryIgac(
            @PathVariable Long id,
            @RequestBody EbsTerritoryIgacUpdateDto dto) {
        ebsService.updateTerritoryIgac(id, dto);
        return ResponseEntity.ok(Map.of("message", "Límites IGAC actualizados"));
    }

    @GetMapping("/households")
    @PreAuthorize(ROLES_EBS)
    public List<EbsHouseholdSummaryDto> listHouseholds(
            @RequestParam("territoryId") Long territoryId,
            @RequestParam(value = "riskLevel", required = false) String riskLevel,
            @RequestParam(value = "visitStatus", required = false) String visitStatus) {
        return ebsService.listHouseholds(territoryId, riskLevel, visitStatus);
    }

    @PostMapping("/home-visits")
    @PreAuthorize(ROLES_EBS)
    public ResponseEntity<Map<String, Object>> createHomeVisit(@Valid @RequestBody EbsHomeVisitRequestDto dto) {
        Long id = ebsService.createHomeVisit(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id, "message", "Visita domiciliaria registrada"));
    }

    @GetMapping("/home-visits")
    @PreAuthorize(ROLES_EBS)
    public List<EbsHomeVisitSummaryDto> listHomeVisits(
            @RequestParam(value = "territoryId", required = false) Long territoryId,
            @RequestParam(value = "professionalId", required = false) Long professionalId,
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo) {
        return ebsService.listHomeVisits(territoryId, professionalId, dateFrom, dateTo);
    }

    @GetMapping("/dashboard")
    @PreAuthorize(ROLES_EBS)
    public EbsDashboardDto getDashboard(
            @RequestParam(value = "diasPeriodo", required = false, defaultValue = "30") Integer diasPeriodo) {
        return ebsService.getDashboard(diasPeriodo);
    }

    @GetMapping("/brigades")
    @PreAuthorize(ROLES_EBS)
    public List<EbsBrigadeDto> listBrigades(@RequestParam(value = "territoryId", required = false) Long territoryId) {
        return ebsService.listBrigades(territoryId);
    }

    @PostMapping("/brigades")
    @PreAuthorize(ROLES_EBS)
    public ResponseEntity<EbsBrigadeDto> createBrigade(@Valid @RequestBody EbsBrigadeDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ebsService.createBrigade(dto));
    }

    @PutMapping("/brigades/{id}")
    @PreAuthorize(ROLES_EBS)
    public EbsBrigadeDto updateBrigade(@PathVariable Long id, @RequestBody EbsBrigadeDto dto) {
        return ebsService.updateBrigade(id, dto);
    }

    @DeleteMapping("/brigades/{id}")
    @PreAuthorize(ROLES_EBS)
    public ResponseEntity<Void> deleteBrigade(@PathVariable Long id) {
        ebsService.deleteBrigade(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/brigades/{id}/team")
    @PreAuthorize(ROLES_EBS)
    public List<Long> getBrigadeTeam(@PathVariable Long id) {
        return ebsService.getBrigadeTeam(id);
    }

    @PutMapping("/brigades/{id}/team")
    @PreAuthorize(ROLES_EBS)
    public ResponseEntity<Map<String, String>> setBrigadeTeam(@PathVariable Long id, @RequestBody List<Long> personalIds) {
        ebsService.setBrigadeTeam(id, personalIds);
        return ResponseEntity.ok(Map.of("message", "Equipo de brigada actualizado"));
    }

    @GetMapping("/alerts")
    @PreAuthorize(ROLES_EBS)
    public List<EbsAlertDto> listAlerts(@RequestParam(value = "status", required = false) String status) {
        return ebsService.listAlerts(status);
    }

    @PostMapping("/alerts")
    @PreAuthorize(ROLES_EBS)
    public ResponseEntity<EbsAlertDto> createAlert(@Valid @RequestBody EbsAlertDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ebsService.createAlert(dto));
    }

    @PatchMapping("/alerts/{id}/status")
    @PreAuthorize(ROLES_EBS)
    public EbsAlertDto updateAlertStatus(@PathVariable Long id, @RequestParam String status) {
        return ebsService.updateAlertStatus(id, status);
    }

    @GetMapping("/reports/data")
    @PreAuthorize(ROLES_EBS)
    public EbsReportDataDto getReportData(
            @RequestParam(value = "reportType", required = false) String reportType,
            @RequestParam(value = "periodFrom", required = false) String periodFrom,
            @RequestParam(value = "periodTo", required = false) String periodTo) {
        return ebsService.getReportData(reportType, periodFrom, periodTo);
    }
}
