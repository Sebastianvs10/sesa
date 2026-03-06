/**
 * Job programado: recordatorios automáticos de citas (24h y 1h antes).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.job;

import com.sesa.salud.entity.master.Empresa;
import com.sesa.salud.repository.master.EmpresaRepository;
import com.sesa.salud.service.RecordatorioCitaService;
import com.sesa.salud.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecordatorioCitaJob {

    private final EmpresaRepository empresaRepository;
    private final RecordatorioCitaService recordatorioCitaService;

    /** Ejecuta cada hora (minuto 0). Procesa todos los tenants activos. */
    @Scheduled(cron = "${sesa.recordatorios.cron:0 0 * * * ?}")
    public void ejecutarRecordatorios() {
        TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
        List<Empresa> empresas = empresaRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getActivo()))
                .toList();
        if (empresas.isEmpty()) {
            return;
        }
        for (Empresa emp : empresas) {
            String schema = emp.getSchemaName();
            try {
                TenantContextHolder.setTenantSchema(schema);
                int n = recordatorioCitaService.procesarRecordatoriosDelTenant();
                if (n > 0) {
                    log.info("Recordatorios enviados en tenant {}: {}", schema, n);
                }
            } catch (Exception e) {
                log.error("Error procesando recordatorios en tenant {}: {}", schema, e.getMessage());
            } finally {
                TenantContextHolder.clear();
            }
        }
    }
}
