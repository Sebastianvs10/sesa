/**
 * Constantes de roles del sistema (RBAC).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.security;

import java.util.Set;

public final class RoleConstants {

    public static final String SUPERADMINISTRADOR = "SUPERADMINISTRADOR";
    public static final String ADMIN = "ADMIN";
    public static final String MEDICO = "MEDICO";
    public static final String ODONTOLOGO = "ODONTOLOGO";
    public static final String BACTERIOLOGO = "BACTERIOLOGO";
    public static final String ENFERMERO = "ENFERMERO";
    public static final String JEFE_ENFERMERIA = "JEFE_ENFERMERIA";
    public static final String AUXILIAR_ENFERMERIA = "AUXILIAR_ENFERMERIA";
    public static final String PSICOLOGO = "PSICOLOGO";
    public static final String REGENTE_FARMACIA = "REGENTE_FARMACIA";
    public static final String RECEPCIONISTA = "RECEPCIONISTA";

    public enum Modulo {
        DASHBOARD, PACIENTES, HISTORIA_CLINICA, LABORATORIOS, IMAGENES, URGENCIAS, HOSPITALIZACION,
        FARMACIA, FACTURACION, CITAS, USUARIOS, PERSONAL, EMPRESAS, NOTIFICACIONES, ROLES
    }

    public enum Accion { VER, CREAR, EDITAR, ELIMINAR, ORDENAR, PRESCRIBIR, DISPENSAR, FACTURAR }

    public static final Set<String> ALL_ROLES = Set.of(
            SUPERADMINISTRADOR, ADMIN, MEDICO, ODONTOLOGO, BACTERIOLOGO, ENFERMERO,
            JEFE_ENFERMERIA, AUXILIAR_ENFERMERIA, PSICOLOGO, REGENTE_FARMACIA, RECEPCIONISTA
    );

    private RoleConstants() {}
}
