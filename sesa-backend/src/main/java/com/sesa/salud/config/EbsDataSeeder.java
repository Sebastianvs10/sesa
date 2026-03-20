/**
 * Carga datos semilla de EBS (microterritorios y hogares) en cada schema de tenant
 * cuando las tablas están vacías, para que la pantalla Territorios muestre datos.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.config;

import com.sesa.salud.repository.master.EmpresaRepository;
import com.sesa.salud.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class EbsDataSeeder implements CommandLineRunner {

    private final DataSource dataSource;
    private final EmpresaRepository empresaRepository;

    @Override
    public void run(String... args) {
        List<String> schemas = empresaRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getActivo()))
                .map(e -> e.getSchemaName())
                .filter(s -> s != null && !TenantContextHolder.PUBLIC.equalsIgnoreCase(s))
                .toList();

        for (String schema : schemas) {
            try {
                seedEbsIfEmpty(schema);
            } catch (Exception ex) {
                log.warn("EbsDataSeeder: no se pudo cargar semilla EBS en schema '{}': {}", schema, ex.getMessage());
            }
        }
    }

    private void seedEbsIfEmpty(String schema) throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            conn.setAutoCommit(false);
            try {
                stmt.execute("SET search_path = '" + schema + "'");

                Long count = null;
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ebs_territories")) {
                    if (rs.next()) count = rs.getLong(1);
                }
                if (count == null || count > 0) {
                    conn.commit();
                    return;
                }

                stmt.execute("""
                    INSERT INTO ebs_territories (code, name, type, active, created_at)
                    VALUES
                      ('EBS-01', 'Microterritorio Norte', 'VEREDA', true, NOW()),
                      ('EBS-02', 'Microterritorio Centro', 'VEREDA', true, NOW()),
                      ('EBS-03', 'Microterritorio Sur', 'VEREDA', true, NOW())
                    """);

                long territoryId;
                try (ResultSet rs = stmt.executeQuery("SELECT id FROM ebs_territories WHERE code = 'EBS-01' LIMIT 1")) {
                    if (!rs.next()) {
                        conn.rollback();
                        return;
                    }
                    territoryId = rs.getLong("id");
                }

                stmt.execute(String.format("""
                    INSERT INTO ebs_households (territory_id, address_text, latitude, longitude, state, risk_level, created_at)
                    VALUES
                      (%d, 'Carrera 50 # 10-20', 6.244200, -75.581200, 'PENDIENTE_VISITA', 'MEDIO', NOW()),
                      (%d, 'Calle 30 # 80-40', 6.250000, -75.565000, 'PENDIENTE_VISITA', 'BAJO', NOW()),
                      (%d, 'Calle 10 # 5-20', 6.248000, -75.572000, 'PENDIENTE_VISITA', NULL, NOW()),
                      (%d, 'Avenida Oriental 45-60', 6.252000, -75.558000, 'EN_SEGUIMIENTO', 'ALTO', NOW()),
                      (%d, 'Carrera 43 # 20-15', 6.240000, -75.590000, 'PENDIENTE_VISITA', 'MEDIO', NOW())
                    """, territoryId, territoryId, territoryId, territoryId, territoryId));

                conn.commit();
                log.info("EbsDataSeeder: datos EBS cargados en schema '{}' (3 microterritorios, 5 hogares de ejemplo)", schema);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
