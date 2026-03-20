/**
 * Servicio de recordatorios automáticos e inteligentes de citas.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

/**
 * Procesa citas en ventanas 24h y 1h, crea notificaciones in-app
 * y opcionalmente dispara push para pacientes con usuario vinculado.
 */
public interface RecordatorioCitaService {

    /**
     * Ejecuta recordatorios para el tenant actual: ventana 24h y ventana 1h.
     * Solo notifica a pacientes que tengan usuarioId (acceso portal/móvil).
     */
    int procesarRecordatoriosDelTenant();
}
