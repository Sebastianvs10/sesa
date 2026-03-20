/**
 * S8: Sugerencia de códigos CIE-10 por motivo de consulta y texto de análisis.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.Cie10SugerenciaDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class Cie10SugerenciaService {

    private static final int MAX_RESULTADOS = 15;
    private static final List<Cie10Entry> CATALOGO = buildCatalogo();

    /**
     * Sugiere códigos CIE-10 a partir del motivo de consulta y del texto de análisis/diagnóstico.
     * Búsqueda por palabras clave en código y descripción; devuelve lista ordenada por relevancia.
     */
    public List<Cie10SugerenciaDto> sugerir(String motivoConsulta, String textoAnalisis) {
        String motivo = motivoConsulta != null ? motivoConsulta.trim() : "";
        String texto = textoAnalisis != null ? textoAnalisis.trim() : "";
        String busqueda = (motivo + " " + texto).trim().toLowerCase(Locale.ROOT);
        if (busqueda.isEmpty()) {
            return List.of();
        }
        String[] palabras = busqueda.split("\\s+");
        List<Cie10SugerenciaDto> resultados = new ArrayList<>();
        for (Cie10Entry e : CATALOGO) {
            int score = 0;
            String codigoLower = e.codigo.toLowerCase(Locale.ROOT);
            String descLower = e.descripcion.toLowerCase(Locale.ROOT);
            for (String p : palabras) {
                if (p.length() < 2) continue;
                if (codigoLower.contains(p)) score += 10;
                if (descLower.contains(p)) score += 5;
            }
            if (score > 0) {
                resultados.add(Cie10SugerenciaDto.builder()
                        .codigo(e.codigo)
                        .descripcion(e.descripcion)
                        .relevancia(score)
                        .build());
            }
        }
        return resultados.stream()
                .sorted(Comparator.comparingInt(Cie10SugerenciaDto::getRelevancia).reversed())
                .limit(MAX_RESULTADOS)
                .collect(Collectors.toList());
    }

    private static List<Cie10Entry> buildCatalogo() {
        List<Cie10Entry> list = new ArrayList<>();
        add(list, "J06.9", "Infección aguda vías respiratorias superiores");
        add(list, "J00", "Rinofaringitis aguda [resfriado común]");
        add(list, "J02.9", "Faringitis aguda no especificada");
        add(list, "I10", "Hipertensión esencial");
        add(list, "E11.9", "Diabetes mellitus tipo 2 sin complicaciones");
        add(list, "K59.0", "Constipación");
        add(list, "K29.7", "Gastritis no especificada");
        add(list, "R51", "Cefalea");
        add(list, "M54.5", "Lumbago no especificado");
        add(list, "M54.2", "Cervicalgia");
        add(list, "M54.6", "Dolor torácico vertebral");
        add(list, "A09", "Diarrea y gastroenteritis presunta infecciosa");
        add(list, "R50.9", "Fiebre no especificada");
        add(list, "Z00.00", "Control general de salud");
        add(list, "Z23", "Encuentro para inmunización");
        add(list, "F41.1", "Trastorno de ansiedad generalizada");
        add(list, "N39.0", "Infección de vías urinarias no especificada");
        add(list, "G43.9", "Migraña no especificada");
        add(list, "E11.65", "Diabetes mellitus tipo 2 con hiperglucemia");
        add(list, "E11.29", "Diabetes mellitus tipo 2 con complicaciones renales");
        add(list, "I25.1", "Enfermedad ateroesclerótica del corazón");
        add(list, "R10.4", "Otros dolores abdominales");
        add(list, "R11", "Náusea y vómito");
        add(list, "J18.9", "Neumonía no especificada");
        add(list, "J20.9", "Bronquitis aguda no especificada");
        add(list, "M79.1", "Mialgia");
        add(list, "R19.0", "Tumefacción masa o prominencia abdominal");
        add(list, "Z20.6", "Contacto con y exposición a tuberculosis");
        add(list, "B34.9", "Infección viral no especificada");
        add(list, "R05", "Tos");
        add(list, "R53", "Malestar y fatiga");
        add(list, "F32.9", "Episodio depresivo no especificado");
        add(list, "K21.9", "Enfermedad del reflujo gastroesofágico sin esofagitis");
        add(list, "R07.4", "Dolor de pecho no especificado");
        add(list, "S00.9", "Traumatismo superficial de la cabeza");
        add(list, "Z11.3", "Examen de despistaje de infecciones de transmisión sexual");
        return list;
    }

    private static void add(List<Cie10Entry> list, String codigo, String descripcion) {
        list.add(new Cie10Entry(codigo, descripcion));
    }

    private record Cie10Entry(String codigo, String descripcion) {}
}
