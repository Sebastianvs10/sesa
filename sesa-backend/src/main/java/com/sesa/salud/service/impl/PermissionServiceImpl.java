/**
 * Implementación de permisos RBAC según matriz de roles SESA.
 * Carga desde BD (role_modulo_permiso) o usa defaults si está vacía.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.entity.master.Role;
import com.sesa.salud.entity.master.RoleModuloPermiso;
import com.sesa.salud.repository.master.RoleModuloPermisoRepository;
import com.sesa.salud.repository.master.RoleRepository;
import com.sesa.salud.security.RoleConstants;
import com.sesa.salud.service.PermissionService;
import com.sesa.salud.tenant.TenantContextHolder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    private final RoleModuloPermisoRepository roleModuloPermisoRepository;
    private final RoleRepository roleRepository;

    /** Matriz rol -> módulo -> acciones permitidas (en memoria, sincronizada con BD) */
    private final Map<String, Map<RoleConstants.Modulo, Set<RoleConstants.Accion>>> matrix = new HashMap<>();

    /** Nombres por defecto de roles del sistema (código → nombre) */
    private static final Map<String, String> ROL_NOMBRES = Map.ofEntries(
        Map.entry(RoleConstants.SUPERADMINISTRADOR,  "Super Usuario"),
        Map.entry(RoleConstants.ADMIN,               "Administrador del Sistema"),
        Map.entry(RoleConstants.MEDICO,              "Médico"),
        Map.entry(RoleConstants.ODONTOLOGO,          "Odontólogo/a"),
        Map.entry(RoleConstants.BACTERIOLOGO,        "Bacteriólogo"),
        Map.entry(RoleConstants.ENFERMERO,           "Enfermero/a"),
        Map.entry(RoleConstants.JEFE_ENFERMERIA,     "Jefe de Enfermería"),
        Map.entry(RoleConstants.AUXILIAR_ENFERMERIA, "Auxiliar de Enfermería"),
        Map.entry(RoleConstants.PSICOLOGO,           "Psicólogo"),
        Map.entry(RoleConstants.REGENTE_FARMACIA,    "Regente de Farmacia"),
        Map.entry(RoleConstants.RECEPCIONISTA,       "Recepcionista"),
        Map.entry(RoleConstants.COORDINADOR_MEDICO,  "Coordinador Médico"),
        Map.entry(RoleConstants.EBS,                  "Profesional EBS"),
        Map.entry(RoleConstants.COORDINADOR_TERRITORIAL, "Coordinador Territorial"),
        Map.entry(RoleConstants.SUPERVISOR_APS,      "Supervisor APS")
    );

    @PostConstruct
    @Transactional
    public void init() {
        // Las tablas de permisos (roles, role_modulo_permiso) están en el schema "public".
        // Forzamos ese schema antes de cualquier operación, ya que en este punto
        // TenantContextHolder puede estar vacío o apuntando a otro schema.
        TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
        try {
            // 1. Asegurar que todos los roles del sistema existen en la tabla `roles`
            ensureSystemRolesExist();

            // 2. Cargar o inicializar la matriz de permisos
            if (roleModuloPermisoRepository.count() > 0) {
                loadFromDb();
                // 3. Seedear permisos de roles nuevos que aún no tienen entradas en BD
                seedMissingRolePermissions();
                // 4. Garantizar que RECEPCIONISTA siempre tenga acceso a PACIENTES
                ensureRecepcionistaTienePacientes();
            } else {
                initMatrix();
                seedToDb();
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar permisos desde BD, usando matriz por defecto: {}", e.getMessage());
            initMatrix();
        }
    }

    /**
     * Garantiza que el rol RECEPCIONISTA tenga siempre el módulo PACIENTES
     * (y acciones por defecto). Si falta, lo añade en memoria y en BD.
     */
    private void ensureRecepcionistaTienePacientes() {
        String rol = RoleConstants.RECEPCIONISTA;
        Map<RoleConstants.Modulo, Set<RoleConstants.Accion>> perms = matrix.get(rol);
        if (perms == null) {
            Map<RoleConstants.Modulo, Set<RoleConstants.Accion>> defaults = buildDefaultMatrix().get(rol);
            if (defaults != null) {
                matrix.put(rol, new HashMap<>(defaults));
                for (RoleConstants.Modulo mod : defaults.keySet()) {
                    roleModuloPermisoRepository.save(
                        RoleModuloPermiso.builder().rol(rol).modulo(mod.name()).build());
                }
                log.info("Permisos de RECEPCIONISTA inicializados (incl. PACIENTES).");
            }
            return;
        }
        if (!perms.containsKey(RoleConstants.Modulo.PACIENTES)) {
            Set<RoleConstants.Accion> acciones = Set.of(
                RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR);
            perms.put(RoleConstants.Modulo.PACIENTES, new HashSet<>(acciones));
            roleModuloPermisoRepository.save(
                RoleModuloPermiso.builder().rol(rol).modulo(RoleConstants.Modulo.PACIENTES.name()).build());
            log.info("Módulo PACIENTES añadido al rol RECEPCIONISTA.");
        }
    }

    /** Inserta en la tabla `roles` cualquier rol del sistema que aún no exista. */
    private void ensureSystemRolesExist() {
        ROL_NOMBRES.forEach((codigo, nombre) -> {
            if (!roleRepository.existsByCodigo(codigo)) {
                roleRepository.save(Role.builder().codigo(codigo).nombre(nombre).build());
                log.info("Rol '{}' creado automáticamente en BD.", codigo);
            }
        });
    }

    /**
     * Tras cargar desde BD, comprueba si hay roles en la matriz por defecto que
     * no tienen entradas en `role_modulo_permiso`. Si los encuentra, los seedea.
     */
    private void seedMissingRolePermissions() {
        Map<String, Map<RoleConstants.Modulo, Set<RoleConstants.Accion>>> defaults = buildDefaultMatrix();
        for (Map.Entry<String, Map<RoleConstants.Modulo, Set<RoleConstants.Accion>>> entry : defaults.entrySet()) {
            String rol = entry.getKey();
            if (!matrix.containsKey(rol)) {
                Map<RoleConstants.Modulo, Set<RoleConstants.Accion>> perms = entry.getValue();
                for (RoleConstants.Modulo mod : perms.keySet()) {
                    roleModuloPermisoRepository.save(
                        RoleModuloPermiso.builder().rol(rol).modulo(mod.name()).build());
                }
                matrix.put(rol, new HashMap<>(perms));
                log.info("Permisos inicializados para nuevo rol '{}': {} módulos.", rol, perms.size());
            }
        }
    }

    private void loadFromDb() {
        matrix.clear();
        List<RoleModuloPermiso> all = roleModuloPermisoRepository.findAll();
        Map<String, Map<RoleConstants.Modulo, Set<RoleConstants.Accion>>> defaults = buildDefaultMatrix();
        for (RoleModuloPermiso p : all) {
            try {
                RoleConstants.Modulo mod = RoleConstants.Modulo.valueOf(p.getModulo());
                Map<RoleConstants.Modulo, Set<RoleConstants.Accion>> perms = matrix
                        .computeIfAbsent(p.getRol().toUpperCase(), k -> new HashMap<>());
                Set<RoleConstants.Accion> actions = defaults
                        .getOrDefault(p.getRol().toUpperCase(), Map.of())
                        .get(mod);
                perms.put(mod, actions != null ? new HashSet<>(actions) : Set.of(RoleConstants.Accion.VER));
            } catch (IllegalArgumentException ignored) {
                // módulo no válido, omitir
            }
        }
        log.info("Matriz de permisos cargada desde BD: {} registros", all.size());
    }

    private void seedToDb() {
        for (Map.Entry<String, Map<RoleConstants.Modulo, Set<RoleConstants.Accion>>> e : matrix.entrySet()) {
            for (RoleConstants.Modulo mod : e.getValue().keySet()) {
                roleModuloPermisoRepository.save(RoleModuloPermiso.builder()
                        .rol(e.getKey())
                        .modulo(mod.name())
                        .build());
            }
        }
        log.info("Matriz de permisos inicial escrita en BD");
    }

    private Map<String, Map<RoleConstants.Modulo, Set<RoleConstants.Accion>>> buildDefaultMatrix() {
        Map<String, Map<RoleConstants.Modulo, Set<RoleConstants.Accion>>> m = new HashMap<>();
        initMatrixInternal(m);
        return m;
    }

    private void initMatrix() {
        initMatrixInternal(matrix);
    }

    private void initMatrixInternal(Map<String, Map<RoleConstants.Modulo, Set<RoleConstants.Accion>>> m) {
        // SUPERADMINISTRADOR: todo
        m.put(RoleConstants.SUPERADMINISTRADOR, new HashMap<>(allPermissions()));

        // ADMIN: todo excepto roles (gestión de roles es solo super)
        Map<RoleConstants.Modulo, Set<RoleConstants.Accion>> admin = new HashMap<>(allPermissions());
        admin.remove(RoleConstants.Modulo.ROLES);
        m.put(RoleConstants.ADMIN, admin);

        // MEDICO: según especificación
        Map<RoleConstants.Modulo, Set<RoleConstants.Accion>> medico = new HashMap<>();
        medico.put(RoleConstants.Modulo.DASHBOARD,         Set.of(RoleConstants.Accion.VER));
        medico.put(RoleConstants.Modulo.PACIENTES,         Set.of(RoleConstants.Accion.VER));
        medico.put(RoleConstants.Modulo.HISTORIA_CLINICA,  Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        medico.put(RoleConstants.Modulo.LABORATORIOS,      Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.ORDENAR));
        medico.put(RoleConstants.Modulo.IMAGENES,          Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.ORDENAR));
        medico.put(RoleConstants.Modulo.URGENCIAS,         Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        medico.put(RoleConstants.Modulo.HOSPITALIZACION,   Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        medico.put(RoleConstants.Modulo.FARMACIA,          Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.PRESCRIBIR));
        medico.put(RoleConstants.Modulo.CITAS,             Set.of(RoleConstants.Accion.VER));
        medico.put(RoleConstants.Modulo.CONSULTA_MEDICA,   Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        medico.put(RoleConstants.Modulo.REPORTES,          Set.of(RoleConstants.Accion.VER));
        medico.put(RoleConstants.Modulo.EBS,               Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        m.put(RoleConstants.MEDICO, medico);

        // ODONTOLOGO: similar a médico para su ámbito
        Map<RoleConstants.Modulo, Set<RoleConstants.Accion>> odontologo = new HashMap<>();
        odontologo.put(RoleConstants.Modulo.DASHBOARD,        Set.of(RoleConstants.Accion.VER));
        odontologo.put(RoleConstants.Modulo.PACIENTES,        Set.of(RoleConstants.Accion.VER));
        odontologo.put(RoleConstants.Modulo.HISTORIA_CLINICA, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        odontologo.put(RoleConstants.Modulo.LABORATORIOS,     Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.ORDENAR));
        odontologo.put(RoleConstants.Modulo.IMAGENES,         Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.ORDENAR));
        odontologo.put(RoleConstants.Modulo.URGENCIAS,        Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        odontologo.put(RoleConstants.Modulo.HOSPITALIZACION,  Set.of(RoleConstants.Accion.VER));
        odontologo.put(RoleConstants.Modulo.FARMACIA,         Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.PRESCRIBIR));
        odontologo.put(RoleConstants.Modulo.CITAS,            Set.of(RoleConstants.Accion.VER));
        odontologo.put(RoleConstants.Modulo.CONSULTA_MEDICA,  Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        odontologo.put(RoleConstants.Modulo.ODONTOLOGIA,      Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        m.put(RoleConstants.ODONTOLOGO, odontologo);

        // BACTERIOLOGO
        m.put(RoleConstants.BACTERIOLOGO, new HashMap<>(Map.of(
                RoleConstants.Modulo.DASHBOARD, Set.of(RoleConstants.Accion.VER),
                RoleConstants.Modulo.PACIENTES, Set.of(RoleConstants.Accion.VER),
                RoleConstants.Modulo.LABORATORIOS, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR)
        )));

        // ENFERMERO
        m.put(RoleConstants.ENFERMERO, new HashMap<>(Map.of(
                RoleConstants.Modulo.PACIENTES, Set.of(RoleConstants.Accion.VER),
                RoleConstants.Modulo.HISTORIA_CLINICA, Set.of(RoleConstants.Accion.VER),
                RoleConstants.Modulo.URGENCIAS, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR),
                RoleConstants.Modulo.HOSPITALIZACION, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR),
                RoleConstants.Modulo.CITAS, Set.of(RoleConstants.Accion.VER)
        )));

        // JEFE_ENFERMERIA: supervisión de enfermería + agenda + consulta médica
        Map<RoleConstants.Modulo, Set<RoleConstants.Accion>> jefeEnf = new HashMap<>();
        jefeEnf.put(RoleConstants.Modulo.DASHBOARD,              Set.of(RoleConstants.Accion.VER));
        jefeEnf.put(RoleConstants.Modulo.PACIENTES,              Set.of(RoleConstants.Accion.VER));
        jefeEnf.put(RoleConstants.Modulo.HISTORIA_CLINICA,       Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        jefeEnf.put(RoleConstants.Modulo.URGENCIAS,              Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        jefeEnf.put(RoleConstants.Modulo.HOSPITALIZACION,        Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR, RoleConstants.Accion.ELIMINAR));
        jefeEnf.put(RoleConstants.Modulo.EVOLUCION_ENFERMERIA,   Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        jefeEnf.put(RoleConstants.Modulo.AGENDA,                 Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        jefeEnf.put(RoleConstants.Modulo.CONSULTA_MEDICA,        Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        m.put(RoleConstants.JEFE_ENFERMERIA, jefeEnf);

        // COORDINADOR_MEDICO: supervisión clínica + reportes + agenda + consulta médica
        Map<RoleConstants.Modulo, Set<RoleConstants.Accion>> coordMed = new HashMap<>();
        coordMed.put(RoleConstants.Modulo.DASHBOARD,        Set.of(RoleConstants.Accion.VER));
        coordMed.put(RoleConstants.Modulo.PACIENTES,        Set.of(RoleConstants.Accion.VER));
        coordMed.put(RoleConstants.Modulo.HISTORIA_CLINICA, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        coordMed.put(RoleConstants.Modulo.LABORATORIOS,     Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.ORDENAR));
        coordMed.put(RoleConstants.Modulo.REPORTES,         Set.of(RoleConstants.Accion.VER));
        coordMed.put(RoleConstants.Modulo.CITAS,            Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        coordMed.put(RoleConstants.Modulo.AGENDA,           Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        coordMed.put(RoleConstants.Modulo.CONSULTA_MEDICA,  Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        coordMed.put(RoleConstants.Modulo.ODONTOLOGIA,      Set.of(RoleConstants.Accion.VER));
        m.put(RoleConstants.COORDINADOR_MEDICO, coordMed);

        // AUXILIAR_ENFERMERIA
        m.put(RoleConstants.AUXILIAR_ENFERMERIA, new HashMap<>(Map.of(
                RoleConstants.Modulo.PACIENTES, Set.of(RoleConstants.Accion.VER),
                RoleConstants.Modulo.URGENCIAS, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR),
                RoleConstants.Modulo.HOSPITALIZACION, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR)
        )));

        // PSICOLOGO
        m.put(RoleConstants.PSICOLOGO, new HashMap<>(Map.of(
                RoleConstants.Modulo.PACIENTES, Set.of(RoleConstants.Accion.VER),
                RoleConstants.Modulo.HISTORIA_CLINICA, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR),
                RoleConstants.Modulo.CITAS, Set.of(RoleConstants.Accion.VER)
        )));

        // REGENTE_FARMACIA
        m.put(RoleConstants.REGENTE_FARMACIA, new HashMap<>(Map.of(
                RoleConstants.Modulo.FARMACIA, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.DISPENSAR, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR),
                RoleConstants.Modulo.PACIENTES, Set.of(RoleConstants.Accion.VER)
        )));

        // RECEPCIONISTA: incluye URGENCIAS porque UrgenciaRegistroController lo permite (GET/POST)
        Map<RoleConstants.Modulo, Set<RoleConstants.Accion>> recepcionista = new HashMap<>();
        recepcionista.put(RoleConstants.Modulo.DASHBOARD,    Set.of(RoleConstants.Accion.VER));
        recepcionista.put(RoleConstants.Modulo.PACIENTES,    Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        recepcionista.put(RoleConstants.Modulo.CITAS,        Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR, RoleConstants.Accion.ELIMINAR));
        recepcionista.put(RoleConstants.Modulo.FACTURACION,  Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.FACTURAR));
        recepcionista.put(RoleConstants.Modulo.URGENCIAS,    Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR));
        m.put(RoleConstants.RECEPCIONISTA, recepcionista);

        // EBS: Profesional EBS — módulo EBS + pacientes, historia, consulta, citas para visitas domiciliarias
        Map<RoleConstants.Modulo, Set<RoleConstants.Accion>> ebs = new HashMap<>();
        ebs.put(RoleConstants.Modulo.DASHBOARD,      Set.of(RoleConstants.Accion.VER));
        ebs.put(RoleConstants.Modulo.PACIENTES,      Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        ebs.put(RoleConstants.Modulo.HISTORIA_CLINICA, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        ebs.put(RoleConstants.Modulo.CONSULTA_MEDICA, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        ebs.put(RoleConstants.Modulo.CITAS,          Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        ebs.put(RoleConstants.Modulo.EBS,            Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        m.put(RoleConstants.EBS, ebs);

        // COORDINADOR_TERRITORIAL: asigna territorios a equipos EBS + ver todo EBS
        Map<RoleConstants.Modulo, Set<RoleConstants.Accion>> coordTerr = new HashMap<>();
        coordTerr.put(RoleConstants.Modulo.DASHBOARD,    Set.of(RoleConstants.Accion.VER));
        coordTerr.put(RoleConstants.Modulo.PACIENTES,     Set.of(RoleConstants.Accion.VER));
        coordTerr.put(RoleConstants.Modulo.EBS,          Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR));
        coordTerr.put(RoleConstants.Modulo.REPORTES,     Set.of(RoleConstants.Accion.VER));
        m.put(RoleConstants.COORDINADOR_TERRITORIAL, coordTerr);

        // SUPERVISOR_APS: dashboards gerenciales EBS, reportes, sin edición operativa
        Map<RoleConstants.Modulo, Set<RoleConstants.Accion>> supAps = new HashMap<>();
        supAps.put(RoleConstants.Modulo.DASHBOARD,   Set.of(RoleConstants.Accion.VER));
        supAps.put(RoleConstants.Modulo.EBS,         Set.of(RoleConstants.Accion.VER));
        supAps.put(RoleConstants.Modulo.REPORTES,    Set.of(RoleConstants.Accion.VER));
        supAps.put(RoleConstants.Modulo.PACIENTES,   Set.of(RoleConstants.Accion.VER));
        m.put(RoleConstants.SUPERVISOR_APS, supAps);
    }

    private static Map<RoleConstants.Modulo, Set<RoleConstants.Accion>> allPermissions() {
        Map<RoleConstants.Modulo, Set<RoleConstants.Accion>> m = new HashMap<>();
        for (RoleConstants.Modulo mod : RoleConstants.Modulo.values()) {
            m.put(mod, EnumSet.allOf(RoleConstants.Accion.class));
        }
        return m;
    }

    @Override
    public boolean hasPermission(String rol, RoleConstants.Modulo modulo, RoleConstants.Accion accion) {
        if (rol == null) return false;
        Map<RoleConstants.Modulo, Set<RoleConstants.Accion>> perms = matrix.get(rol.toUpperCase());
        if (perms == null) return false;
        Set<RoleConstants.Accion> actions = perms.get(modulo);
        return actions != null && actions.contains(accion);
    }

    @Override
    public boolean hasAnyPermission(Set<String> roles, RoleConstants.Modulo modulo, RoleConstants.Accion accion) {
        if (roles == null || roles.isEmpty()) return false;
        for (String r : roles) {
            if (hasPermission(r, modulo, accion)) return true;
        }
        return false;
    }

    @Override
    public boolean canAccessModule(Set<String> roles, RoleConstants.Modulo modulo) {
        return hasAnyPermission(roles, modulo, RoleConstants.Accion.VER);
    }

    @Override
    public Set<RoleConstants.Modulo> getAccessibleModules(Set<String> roles) {
        Set<RoleConstants.Modulo> result = new HashSet<>();
        for (RoleConstants.Modulo mod : RoleConstants.Modulo.values()) {
            if (canAccessModule(roles, mod)) result.add(mod);
        }
        return result;
    }

    @Override
    @Transactional
    public void updateModulosForRole(String rol, Set<RoleConstants.Modulo> modulos) {
        if (rol == null) return;
        // Los permisos se guardan en public; asegurar schema correcto antes de escribir
        TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
        String rolUpper = rol.toUpperCase();
        if (!RoleConstants.ALL_ROLES.contains(rolUpper) && !roleRepository.existsByCodigo(rolUpper)) {
            throw new IllegalArgumentException("Rol no válido: " + rol);
        }
        roleModuloPermisoRepository.deleteByRol(rolUpper);
        Map<RoleConstants.Modulo, Set<RoleConstants.Accion>> defaults = buildDefaultMatrix().getOrDefault(rolUpper, Map.of());
        Map<RoleConstants.Modulo, Set<RoleConstants.Accion>> newPerms = new HashMap<>();
        for (RoleConstants.Modulo mod : modulos) {
            roleModuloPermisoRepository.save(RoleModuloPermiso.builder()
                    .rol(rolUpper)
                    .modulo(mod.name())
                    .build());
            Set<RoleConstants.Accion> actions = defaults.getOrDefault(mod, Set.of(RoleConstants.Accion.VER));
            newPerms.put(mod, new HashSet<>(actions));
        }
        matrix.put(rolUpper, newPerms);
        log.info("Permisos actualizados para rol {}: {} módulos", rolUpper, modulos.size());
    }
}
