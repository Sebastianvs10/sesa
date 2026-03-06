/**
 * Constantes de roles del sistema (RBAC).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.security;

import java.util.Set;

public final class RoleConstants {

    public static final String SUPERADMINISTRADOR    = "SUPERADMINISTRADOR";
    public static final String ADMIN                 = "ADMIN";
    public static final String MEDICO                = "MEDICO";
    public static final String ODONTOLOGO            = "ODONTOLOGO";
    public static final String BACTERIOLOGO          = "BACTERIOLOGO";
    public static final String ENFERMERO             = "ENFERMERO";
    public static final String JEFE_ENFERMERIA       = "JEFE_ENFERMERIA";
    public static final String AUXILIAR_ENFERMERIA   = "AUXILIAR_ENFERMERIA";
    public static final String PSICOLOGO             = "PSICOLOGO";
    public static final String REGENTE_FARMACIA      = "REGENTE_FARMACIA";
    public static final String RECEPCIONISTA         = "RECEPCIONISTA";
    public static final String COORDINADOR_MEDICO    = "COORDINADOR_MEDICO";
    /** Profesional de Equipos Básicos de Salud (visitas domiciliarias, APS). */
    public static final String EBS                    = "EBS";
    /** Coordinador territorial: asigna microterritorios a equipos EBS. */
    public static final String COORDINADOR_TERRITORIAL = "COORDINADOR_TERRITORIAL";
    /** Supervisor de Atención Primaria en Salud: dashboards y supervisión EBS. */
    public static final String SUPERVISOR_APS        = "SUPERVISOR_APS";

    public enum Modulo {
        DASHBOARD, PACIENTES, HISTORIA_CLINICA, LABORATORIOS, IMAGENES, URGENCIAS, HOSPITALIZACION,
        FARMACIA, FACTURACION, CITAS, USUARIOS, PERSONAL, EMPRESAS, NOTIFICACIONES, ROLES,
        REPORTES, AGENDA, EVOLUCION_ENFERMERIA, CONSULTA_MEDICA, ODONTOLOGIA,
        EBS
    }

    public enum Accion { VER, CREAR, EDITAR, ELIMINAR, ORDENAR, PRESCRIBIR, DISPENSAR, FACTURAR }

    public static final Set<String> ALL_ROLES = Set.of(
            SUPERADMINISTRADOR, ADMIN, MEDICO, ODONTOLOGO, BACTERIOLOGO, ENFERMERO,
            JEFE_ENFERMERIA, AUXILIAR_ENFERMERIA, PSICOLOGO, REGENTE_FARMACIA, RECEPCIONISTA,
            COORDINADOR_MEDICO, EBS, COORDINADOR_TERRITORIAL, SUPERVISOR_APS
    );

    private RoleConstants() {}
}
