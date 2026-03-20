/**
 * S14: Implementación de interpretación de resultados en lenguaje sencillo (plantillas por tipo/rango).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.service.InterpretacionResultadoService;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InterpretacionResultadoServiceImpl implements InterpretacionResultadoService {

    private static final String DEFAULT = "Sus resultados han sido registrados. El equipo de salud los revisará. Si tiene dudas o síntomas, consulte a su médico.";
    private static final String DENTRO_ESPERADO = "Los valores se encuentran dentro del rango esperado. Mantenga sus controles habituales.";
    private static final String REQUIERE_SEGUIMIENTO = "Algunos valores requieren seguimiento. Le recomendamos agendar una cita con su médico.";
    private static final String CONSULTE_MEDICO = "Consulte a su médico para la interpretación de estos resultados y las recomendaciones adecuadas.";

    @Override
    public String getInterpretacionLenguajeSencillo(String tipoOrden, String resultado) {
        if (resultado == null || resultado.isBlank()) {
            return DEFAULT;
        }
        String tipo = tipoOrden != null ? tipoOrden.toUpperCase() : "";
        String res = resultado.trim();
        if (tipo.contains("LABORATORIO") || tipo.contains("LAB")) {
            return interpretarLaboratorio(res);
        }
        if (tipo.contains("IMAGEN") || tipo.contains("IMAGENOLOGIA")) {
            return "El informe de imagen ha sido registrado. Su médico le explicará los hallazgos en la próxima consulta.";
        }
        return DEFAULT;
    }

    private String interpretarLaboratorio(String resultado) {
        if (resultado.length() > 500) {
            return CONSULTE_MEDICO;
        }
        String upper = resultado.toUpperCase();
        if (upper.contains("GLICEMIA") || upper.contains("GLUCOSA")) {
            Double valor = extraerPrimerNumero(resultado);
            if (valor != null) {
                if (valor >= 70 && valor <= 100) return DENTRO_ESPERADO;
                if (valor >= 100 && valor <= 125) return "El valor de glicemia sugiere seguimiento. Consulte a su médico para recomendaciones.";
                if (valor < 70 || valor > 125) return CONSULTE_MEDICO;
            }
        }
        if (upper.contains("HEMOGLOBINA") || upper.contains("HEMOGRAMA")) {
            Double valor = extraerPrimerNumero(resultado);
            if (valor != null) {
                if (valor >= 12 && valor <= 17) return DENTRO_ESPERADO;
                return REQUIERE_SEGUIMIENTO;
            }
        }
        if (upper.contains("CREATININA")) {
            Double valor = extraerPrimerNumero(resultado);
            if (valor != null && valor >= 0.6 && valor <= 1.2) return DENTRO_ESPERADO;
            return REQUIERE_SEGUIMIENTO;
        }
        return DEFAULT;
    }

    private static Double extraerPrimerNumero(String texto) {
        Pattern p = Pattern.compile("[0-9]+([.,][0-9]+)?");
        Matcher m = p.matcher(texto);
        if (m.find()) {
            String s = m.group().replace(',', '.');
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
