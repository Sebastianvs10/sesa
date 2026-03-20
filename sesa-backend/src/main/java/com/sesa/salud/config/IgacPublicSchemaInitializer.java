/**
 * Crea tablas catálogo IGAC en schema public y carga datos semilla (DANE).
 * Descarga recomendada: usar capa oficial IGAC y cargar vía script o import.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class IgacPublicSchemaInitializer implements CommandLineRunner {

    private final DataSource dataSource;

    private static final String DDL_DEPARTAMENTOS = """
        CREATE TABLE IF NOT EXISTS public.igac_departamentos (
            id           BIGSERIAL PRIMARY KEY,
            codigo_dane   VARCHAR(2) NOT NULL UNIQUE,
            nombre        VARCHAR(120) NOT NULL
        )
        """;
    private static final String DDL_MUNICIPIOS = """
        CREATE TABLE IF NOT EXISTS public.igac_municipios (
            id                   BIGSERIAL PRIMARY KEY,
            codigo_dane          VARCHAR(5) NOT NULL UNIQUE,
            departamento_codigo  VARCHAR(2) NOT NULL,
            nombre               VARCHAR(120) NOT NULL
        )
        """;
    private static final String DDL_VEREDAS = """
        CREATE TABLE IF NOT EXISTS public.igac_veredas (
            id              BIGSERIAL PRIMARY KEY,
            codigo          VARCHAR(20) NOT NULL,
            municipio_codigo VARCHAR(5) NOT NULL,
            nombre          VARCHAR(200) NOT NULL,
            geometry_json   TEXT
        );
        CREATE INDEX IF NOT EXISTS idx_igac_veredas_municipio ON public.igac_veredas(municipio_codigo);
        """;

    @Override
    public void run(String... args) {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(DDL_DEPARTAMENTOS);
            stmt.execute(DDL_MUNICIPIOS);
            stmt.execute("CREATE TABLE IF NOT EXISTS public.igac_veredas (id BIGSERIAL PRIMARY KEY, codigo VARCHAR(20) NOT NULL, municipio_codigo VARCHAR(5) NOT NULL, nombre VARCHAR(200) NOT NULL, geometry_json TEXT)");
            try {
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_igac_veredas_municipio ON public.igac_veredas(municipio_codigo)");
            } catch (Exception ignored) {}
            log.info("IGAC: tablas public.igac_* creadas/verificadas");
        } catch (Exception e) {
            log.error("IGAC: error creando tablas: {}", e.getMessage());
            return;
        }

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Long countDep = jdbc.queryForObject("SELECT COUNT(*) FROM public.igac_departamentos", Long.class);
        if (countDep == null || countDep == 0) {
            seedDepartamentos(jdbc);
            seedMunicipios(jdbc);
            seedVeredas(jdbc);
            log.info("IGAC: datos semilla (DANE) cargados. Para datos completos, importe desde IGAC.");
        }
    }

    private void seedDepartamentos(JdbcTemplate jdbc) {
        List<Object[]> rows = List.of(
            new Object[]{"05", "Antioquia"},
            new Object[]{"08", "Atlántico"},
            new Object[]{"11", "Bogotá D.C."},
            new Object[]{"17", "Caldas"},
            new Object[]{"18", "Caquetá"},
            new Object[]{"19", "Cauca"},
            new Object[]{"25", "Cundinamarca"},
            new Object[]{"41", "Huila"},
            new Object[]{"47", "Magdalena"},
            new Object[]{"52", "Nariño"},
            new Object[]{"54", "Norte de Santander"},
            new Object[]{"63", "Quindío"},
            new Object[]{"66", "Risaralda"},
            new Object[]{"68", "Santander"},
            new Object[]{"73", "Tolima"},
            new Object[]{"76", "Valle del Cauca"}
        );
        for (Object[] row : rows) {
            jdbc.update("INSERT INTO public.igac_departamentos (codigo_dane, nombre) VALUES (?, ?) ON CONFLICT (codigo_dane) DO NOTHING", row[0], row[1]);
        }
    }

    private void seedMunicipios(JdbcTemplate jdbc) {
        List<Object[]> rows = List.of(
            new Object[]{"05001", "05", "Medellín"},
            new Object[]{"05002", "05", "Abejorral"},
            new Object[]{"05004", "05", "Abriaquí"},
            new Object[]{"08001", "08", "Barranquilla"},
            new Object[]{"08078", "08", "Galapa"},
            new Object[]{"11001", "11", "Bogotá, D.C."},
            new Object[]{"17001", "17", "Manizales"},
            new Object[]{"25001", "25", "Agua de Dios"},
            new Object[]{"25899", "25", "Zipacón"},
            new Object[]{"66001", "66", "Pereira"},
            new Object[]{"76001", "76", "Cali"},
            new Object[]{"76834", "76", "Yumbo"}
        );
        for (Object[] row : rows) {
            jdbc.update("INSERT INTO public.igac_municipios (codigo_dane, departamento_codigo, nombre) VALUES (?, ?, ?) ON CONFLICT (codigo_dane) DO NOTHING", row[0], row[1], row[2]);
        }
    }

    private void seedVeredas(JdbcTemplate jdbc) {
        List<Object[]> rows = List.of(
            new Object[]{"05001-001", "05001", "Santa Elena"},
            new Object[]{"05001-002", "05001", "San Antonio de Prado"},
            new Object[]{"05001-003", "05001", "Palmitas"},
            new Object[]{"66001-001", "66001", "La Florida"},
            new Object[]{"66001-002", "66001", "Tribunas"},
            new Object[]{"76001-001", "76001", "Pance"},
            new Object[]{"76001-002", "76001", "Felidia"}
        );
        for (Object[] row : rows) {
            jdbc.update("INSERT INTO public.igac_veredas (codigo, municipio_codigo, nombre) VALUES (?, ?, ?)", row[0], row[1], row[2]);
        }
    }
}
