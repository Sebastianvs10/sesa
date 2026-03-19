/**
 * Servicio EBS: territorios, hogares y visitas domiciliarias.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.*;

import java.time.Instant;
import java.util.List;

public interface EbsService {

    List<EbsTerritorySummaryDto> listTerritories(String riskLevel);

    EbsTerritorySummaryDto createTerritory(EbsTerritoryCreateDto dto);

    void updateTerritoryIgac(Long territoryId, EbsTerritoryIgacUpdateDto dto);

    List<Long> getTerritoryTeam(Long territoryId);

    void setTerritoryTeam(Long territoryId, List<Long> personalIds);

    List<EbsHouseholdSummaryDto> listHouseholds(Long territoryId, String riskLevel, String visitStatus);

    Long createHomeVisit(EbsHomeVisitRequestDto dto);

    List<EbsHomeVisitSummaryDto> listHomeVisits(Long territoryId, Long professionalId, Instant dateFrom, Instant dateTo);

    EbsDashboardDto getDashboard(Integer diasPeriodo);

    List<EbsBrigadeDto> listBrigades(Long territoryId);

    EbsBrigadeDto createBrigade(EbsBrigadeDto dto);

    EbsBrigadeDto updateBrigade(Long id, EbsBrigadeDto dto);

    void deleteBrigade(Long id);

    List<Long> getBrigadeTeam(Long brigadeId);

    void setBrigadeTeam(Long brigadeId, List<Long> personalIds);

    List<EbsAlertDto> listAlerts(String status);

    EbsAlertDto createAlert(EbsAlertDto dto);

    EbsAlertDto updateAlertStatus(Long id, String status);

    EbsReportDataDto getReportData(String reportType, String periodFrom, String periodTo);

    /** S13: Visitas creadas después de una fecha (para pull en sincronización). */
    List<EbsHomeVisitSummaryDto> listPendientesSincronizar(Instant desde);

    /** S13: Aplica sincronización (last-write-wins) y devuelve conflictos si los hay. */
    VisitaEbsSyncResponseDto sincronizarVisitas(List<VisitaEbsSyncDto> visitas);
}
