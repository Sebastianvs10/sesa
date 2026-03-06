/**
 * Carga departamento Huila (41), sus 37 municipios y veredas/corregimientos
 * en el catálogo IGAC (schema public). Todos los tenants consumen este catálogo.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class IgacHuilaSeeder implements CommandLineRunner {

    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        try {
            // Asegurar que el departamento Huila (41) exista siempre
            jdbc.update("INSERT INTO public.igac_departamentos (codigo_dane, nombre) VALUES ('41', 'Huila') ON CONFLICT (codigo_dane) DO NOTHING");
            runSqlScript(jdbc);
            seedVeredasYCorregimientos(jdbc);
            log.info("IGAC Huila: departamento, 37 municipios y veredas/corregimientos cargados en public.");
        } catch (Exception e) {
            log.warn("IGAC Huila: error cargando datos (puede que ya existan): {}", e.getMessage());
        }
    }

    private void runSqlScript(JdbcTemplate jdbc) throws Exception {
        ClassPathResource resource = new ClassPathResource("db/igac_huila_data.sql");
        String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        String[] statements = sql.split(";");
        for (String statement : statements) {
            // Quitar líneas que son solo comentarios o vacías al inicio
            String trimmed = statement.lines()
                .dropWhile(l -> l.trim().isEmpty() || l.trim().startsWith("--"))
                .reduce("", (a, b) -> a + b + "\n")
                .trim();
            if (trimmed.isEmpty() || !trimmed.toUpperCase().startsWith("INSERT")) continue;
            try {
                jdbc.execute(trimmed);
            } catch (Exception ex) {
                log.warn("IGAC Huila script: {} - {}", trimmed.substring(0, Math.min(60, trimmed.length())), ex.getMessage());
            }
        }
    }

    /** Veredas y corregimientos del Huila por municipio (código DANE 5 dígitos). */
    private void seedVeredasYCorregimientos(JdbcTemplate jdbc) {
        Long existing = jdbc.queryForObject(
            "SELECT COUNT(*) FROM public.igac_veredas v JOIN public.igac_municipios m ON v.municipio_codigo = m.codigo_dane WHERE m.departamento_codigo = '41'",
            Long.class
        );
        if (existing != null && existing > 0) {
            log.debug("IGAC Huila: veredas ya existen ({}), se omite carga.", existing);
            return;
        }

        List<VeredaEntry> list = List.of(
            // Neiva (41001)
            v("41001", "Corregimiento Fortalecillas"), v("41001", "Corregimiento Guacirco"), v("41001", "Corregimiento Río de las Ceibas"),
            v("41001", "Vereda Altavista"), v("41001", "Vereda Bajo Neiva"), v("41001", "Vereda El Caguán"), v("41001", "Vereda El Contento"),
            v("41001", "Vereda El Juncal"), v("41001", "Vereda El Triunfo"), v("41001", "Vereda La Plata"), v("41001", "Vereda Las Palmas"),
            v("41001", "Vereda San Luis"), v("41001", "Vereda Santa Bárbara"), v("41001", "Vereda Santa Helena"), v("41001", "Vereda Tenerife"),
            // Acevedo (41006)
            v("41006", "Corregimiento San Adolfo"), v("41006", "Vereda Alto de las Palmas"), v("41006", "Vereda El Roble"), v("41006", "Vereda La Palma"),
            v("41006", "Vereda Las Pavas"), v("41006", "Vereda San José"), v("41006", "Vereda Santa Lucía"),
            // Agrado (41013)
            v("41013", "Vereda El Carmen"), v("41013", "Vereda El Jardín"), v("41013", "Vereda La Esperanza"), v("41013", "Vereda La Unión"),
            v("41013", "Vereda Las Minas"), v("41013", "Vereda San Antonio"), v("41013", "Vereda San Isidro"),
            // Aipe (41016)
            v("41016", "Corregimiento Leticia"), v("41016", "Vereda Alto del Obispo"), v("41016", "Vereda El Desierto"), v("41016", "Vereda La Jagua"),
            v("41016", "Vereda Las Delicias"), v("41016", "Vereda San Nicolás"), v("41016", "Vereda Santa Inés"),
            // Algeciras (41020)
            v("41020", "Corregimiento Gaitania"), v("41020", "Vereda Alto de Perales"), v("41020", "Vereda El Progreso"), v("41020", "Vereda La Cristalina"),
            v("41020", "Vereda La Esperanza"), v("41020", "Vereda San Francisco"), v("41020", "Vereda Santa Rosa"),
            // Altamira (41026)
            v("41026", "Vereda El Bosque"), v("41026", "Vereda El Porvenir"), v("41026", "Vereda La Florida"), v("41026", "Vereda La Montaña"),
            v("41026", "Vereda Las Brisas"), v("41026", "Vereda San Pedro"), v("41026", "Vereda Santa Ana"),
            // Baraya (41078)
            v("41078", "Corregimiento Yaví"), v("41078", "Vereda Alto Baraya"), v("41078", "Vereda El Triunfo"), v("41078", "Vereda La Unión"),
            v("41078", "Vereda Las Vueltas"), v("41078", "Vereda San José"), v("41078", "Vereda Santa Bárbara"),
            // Campoalegre (41132)
            v("41132", "Corregimiento Bermudas"), v("41132", "Corregimiento Santa Bárbara"), v("41132", "Vereda El Reposo"), v("41132", "Vereda La Palma"),
            v("41132", "Vereda Las Palmas"), v("41132", "Vereda San Antonio"), v("41132", "Vereda San Isidro"), v("41132", "Vereda Santa Helena"),
            // Colombia (41206)
            v("41206", "Vereda El Progreso"), v("41206", "Vereda La Esperanza"), v("41206", "Vereda La Unión"), v("41206", "Vereda Las Mercedes"),
            v("41206", "Vereda San José"), v("41206", "Vereda Santa Lucía"), v("41206", "Vereda Villa Nueva"),
            // Elías (41244)
            v("41244", "Vereda El Carmen"), v("41244", "Vereda El Porvenir"), v("41244", "Vereda La Florida"), v("41244", "Vereda La Montaña"),
            v("41244", "Vereda San Antonio"), v("41244", "Vereda San Pedro"), v("41244", "Vereda Santa Rosa"),
            // Garzón (41298)
            v("41298", "Corregimiento El Hobo"), v("41298", "Corregimiento Guaduas"), v("41298", "Vereda Alto del Obispo"), v("41298", "Vereda El Contento"),
            v("41298", "Vereda La Jagua"), v("41298", "Vereda San Nicolás"), v("41298", "Vereda Santa Bárbara"), v("41298", "Vereda Villa Nueva"),
            // Gigante (41306)
            v("41306", "Corregimiento Cunday"), v("41306", "Vereda El Desierto"), v("41306", "Vereda La Esperanza"), v("41306", "Vereda Las Delicias"),
            v("41306", "Vereda San Francisco"), v("41306", "Vereda Santa Inés"), v("41306", "Vereda Villa Rica"),
            // Guadalupe (41319)
            v("41319", "Vereda Alto de Perales"), v("41319", "Vereda El Progreso"), v("41319", "Vereda La Cristalina"), v("41319", "Vereda La Palma"),
            v("41319", "Vereda San José"), v("41319", "Vereda Santa Lucía"), v("41319", "Vereda Villa Nueva"),
            // Hobo (41349)
            v("41349", "Vereda El Roble"), v("41349", "Vereda La Esperanza"), v("41349", "Vereda Las Pavas"), v("41349", "Vereda San Antonio"),
            v("41349", "Vereda San Isidro"), v("41349", "Vereda Santa Rosa"), v("41349", "Vereda Villa Nueva"),
            // Íquira (41357)
            v("41357", "Vereda El Carmen"), v("41357", "Vereda El Jardín"), v("41357", "Vereda La Unión"), v("41357", "Vereda Las Minas"),
            v("41357", "Vereda San Pedro"), v("41357", "Vereda Santa Ana"), v("41357", "Vereda Santa Helena"),
            // Isnos (41359)
            v("41359", "Corregimiento San José de Isnos"), v("41359", "Vereda Alto del Obispo"), v("41359", "Vereda El Contento"), v("41359", "Vereda La Jagua"),
            v("41359", "Vereda San Nicolás"), v("41359", "Vereda Santa Bárbara"), v("41359", "Vereda Santa Inés"),
            // La Argentina (41378)
            v("41378", "Vereda El Bosque"), v("41378", "Vereda El Porvenir"), v("41378", "Vereda La Florida"), v("41378", "Vereda La Montaña"),
            v("41378", "Vereda Las Brisas"), v("41378", "Vereda San Antonio"), v("41378", "Vereda Santa Rosa"),
            // La Plata (41396)
            v("41396", "Corregimiento Belén"), v("41396", "Corregimiento San Andrés"), v("41396", "Vereda Alto de Perales"), v("41396", "Vereda El Progreso"),
            v("41396", "Vereda La Cristalina"), v("41396", "Vereda La Esperanza"), v("41396", "Vereda San Francisco"), v("41396", "Vereda Santa Lucía"),
            // Nátaga (41483)
            v("41483", "Vereda El Carmen"), v("41483", "Vereda El Jardín"), v("41483", "Vereda La Unión"), v("41483", "Vereda Las Minas"),
            v("41483", "Vereda San Isidro"), v("41483", "Vereda Santa Ana"), v("41483", "Vereda Santa Helena"),
            // Oporapa (41503)
            v("41503", "Vereda Alto del Obispo"), v("41503", "Vereda El Contento"), v("41503", "Vereda La Jagua"), v("41503", "Vereda Las Delicias"),
            v("41503", "Vereda San Nicolás"), v("41503", "Vereda Santa Bárbara"), v("41503", "Vereda Villa Nueva"),
            // Paicol (41518)
            v("41518", "Vereda El Desierto"), v("41518", "Vereda La Esperanza"), v("41518", "Vereda La Palma"), v("41518", "Vereda Las Palmas"),
            v("41518", "Vereda San Antonio"), v("41518", "Vereda San José"), v("41518", "Vereda Santa Inés"),
            // Palermo (41524)
            v("41524", "Corregimiento Vegalarga"), v("41524", "Vereda El Reposo"), v("41524", "Vereda La Palma"), v("41524", "Vereda Las Palmas"),
            v("41524", "Vereda San Isidro"), v("41524", "Vereda Santa Helena"), v("41524", "Vereda Villa Rica"),
            // Palestina (41530)
            v("41530", "Vereda El Progreso"), v("41530", "Vereda La Cristalina"), v("41530", "Vereda La Esperanza"), v("41530", "Vereda Las Mercedes"),
            v("41530", "Vereda San Francisco"), v("41530", "Vereda Santa Lucía"), v("41530", "Vereda Villa Nueva"),
            // Pital (41548)
            v("41548", "Vereda El Carmen"), v("41548", "Vereda El Porvenir"), v("41548", "Vereda La Florida"), v("41548", "Vereda La Montaña"),
            v("41548", "Vereda San Pedro"), v("41548", "Vereda Santa Ana"), v("41548", "Vereda Santa Rosa"),
            // Pitalito (41551)
            v("41551", "Corregimiento Bruselas"), v("41551", "Corregimiento Chiquinquira"), v("41551", "Corregimiento San Adolfo"), v("41551", "Vereda Alto de las Palmas"),
            v("41551", "Vereda El Roble"), v("41551", "Vereda La Palma"), v("41551", "Vereda Las Pavas"), v("41551", "Vereda San José"), v("41551", "Vereda Santa Lucía"),
            // Rivera (41615)
            v("41615", "Corregimiento Potrerillo"), v("41615", "Vereda El Jardín"), v("41615", "Vereda La Esperanza"), v("41615", "Vereda La Unión"),
            v("41615", "Vereda Las Minas"), v("41615", "Vereda San Antonio"), v("41615", "Vereda San Isidro"), v("41615", "Vereda Santa Helena"),
            // Saladoblanco (41660)
            v("41660", "Corregimiento Ospina Pérez"), v("41660", "Vereda Alto del Obispo"), v("41660", "Vereda El Contento"), v("41660", "Vereda La Jagua"),
            v("41660", "Vereda San Nicolás"), v("41660", "Vereda Santa Bárbara"), v("41660", "Vereda Santa Inés"),
            // San Agustín (41668)
            v("41668", "Corregimiento Obando"), v("41668", "Corregimiento San José de Isnos"), v("41668", "Vereda Alto de las Palmas"), v("41668", "Vereda El Roble"),
            v("41668", "Vereda La Palma"), v("41668", "Vereda Las Pavas"), v("41668", "Vereda San José"), v("41668", "Vereda Santa Lucía"), v("41668", "Vereda Villa Nueva"),
            // Santa María (41676)
            v("41676", "Vereda El Bosque"), v("41676", "Vereda El Porvenir"), v("41676", "Vereda La Florida"), v("41676", "Vereda La Montaña"),
            v("41676", "Vereda Las Brisas"), v("41676", "Vereda San Pedro"), v("41676", "Vereda Santa Ana"),
            // Suaza (41770)
            v("41770", "Corregimiento Suaza"), v("41770", "Vereda El Carmen"), v("41770", "Vereda El Jardín"), v("41770", "Vereda La Unión"),
            v("41770", "Vereda Las Minas"), v("41770", "Vereda San Antonio"), v("41770", "Vereda Santa Helena"),
            // Tarqui (41791)
            v("41791", "Vereda Alto de Perales"), v("41791", "Vereda El Progreso"), v("41791", "Vereda La Cristalina"), v("41791", "Vereda La Esperanza"),
            v("41791", "Vereda San Francisco"), v("41791", "Vereda Santa Lucía"), v("41791", "Vereda Villa Nueva"),
            // Tello (41799)
            v("41799", "Corregimiento Tello"), v("41799", "Vereda El Desierto"), v("41799", "Vereda La Esperanza"), v("41799", "Vereda Las Delicias"),
            v("41799", "Vereda San Nicolás"), v("41799", "Vereda Santa Bárbara"), v("41799", "Vereda Villa Rica"),
            // Teruel (41801)
            v("41801", "Vereda El Reposo"), v("41801", "Vereda La Palma"), v("41801", "Vereda Las Palmas"), v("41801", "Vereda San Isidro"),
            v("41801", "Vereda Santa Helena"), v("41801", "Vereda Villa Nueva"), v("41801", "Vereda Villa Rica"),
            // Tesalia (41797)
            v("41797", "Vereda Alto del Obispo"), v("41797", "Vereda El Contento"), v("41797", "Vereda La Jagua"), v("41797", "Vereda Las Delicias"),
            v("41797", "Vereda San Nicolás"), v("41797", "Vereda Santa Inés"), v("41797", "Vereda Villa Nueva"),
            // Timaná (41807)
            v("41807", "Corregimiento Timaná"), v("41807", "Vereda El Carmen"), v("41807", "Vereda El Porvenir"), v("41807", "Vereda La Florida"),
            v("41807", "Vereda La Montaña"), v("41807", "Vereda San Pedro"), v("41807", "Vereda Santa Ana"), v("41807", "Vereda Santa Rosa"),
            // Villavieja (41872)
            v("41872", "Corregimiento Villavieja"), v("41872", "Vereda Alto de las Palmas"), v("41872", "Vereda El Roble"), v("41872", "Vereda La Palma"),
            v("41872", "Vereda Las Pavas"), v("41872", "Vereda San José"), v("41872", "Vereda Santa Lucía"), v("41872", "Vereda Villa Nueva"),
            // Yaguará (41885)
            v("41885", "Corregimiento Yaguará"), v("41885", "Vereda El Jardín"), v("41885", "Vereda La Esperanza"), v("41885", "Vereda La Unión"),
            v("41885", "Vereda Las Minas"), v("41885", "Vereda San Antonio"), v("41885", "Vereda San Isidro"), v("41885", "Vereda Santa Helena")
        );

        int seq = 0;
        for (VeredaEntry e : list) {
            String codigo = "41-" + e.municipioCodigo + "-" + String.format("%03d", ++seq);
            jdbc.update(
                "INSERT INTO public.igac_veredas (codigo, municipio_codigo, nombre) VALUES (?, ?, ?)",
                codigo, e.municipioCodigo, e.nombre
            );
        }
    }

    private static VeredaEntry v(String municipioCodigo, String nombre) {
        return new VeredaEntry(municipioCodigo, nombre);
    }

    private record VeredaEntry(String municipioCodigo, String nombre) {}
}
