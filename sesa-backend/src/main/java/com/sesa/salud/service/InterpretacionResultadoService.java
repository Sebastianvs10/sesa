/**
 * S14: Servicio de interpretación de resultados en lenguaje sencillo para el portal del paciente.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

public interface InterpretacionResultadoService {

    /**
     * Devuelve un texto breve en lenguaje sencillo según el tipo de orden y el resultado.
     * Por ejemplo: "Dentro de lo esperado", "Requiere seguimiento", "Consulte a su médico".
     */
    String getInterpretacionLenguajeSencillo(String tipoOrden, String resultado);
}
