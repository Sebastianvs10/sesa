/**
 * Implementación de permisos RBAC según matriz de roles SESA.
 * Carga desde BD (role_modulo_permiso) o usa defaults si está vacía.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.entity.master.RoleModuloPermiso;
import com.sesa.salud.repository.master.RoleModuloPermisoRepository;
import com.sesa.salud.repository.master.RoleRepository;
import com.sesa.salud.security.RoleConstants;
import com.sesa.salud.service.PermissionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    private final RoleModuloPermisoRepository roleModuloPermisoRepository;
    private final RoleRepository roleRepository;

    /** Matriz rol -> módulo -> acciones permitidas (en memoria, sincronizada con BD) */
    private final Map<String, Map<RoleConstants.Modulo, Set<RoleConstants.Accion>>> matrix = new HashMap<>();

    @PostConstruct
    public void init() {
        try {
            if (roleModuloPermisoRepository.count() > 0) {
                loadFromDb();
            } else {
                initMatrix();
                seedToDb();
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar permisos desde BD, usando matriz por defecto: {}", e.getMessage());
            initMatrix();
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
        m.put(RoleConstants.MEDICO, new HashMap<>(Map.of(
                RoleConstants.Modulo.DASHBOARD, Set.of(RoleConstants.Accion.VER),
                RoleConstants.Modulo.PACIENTES, Set.of(RoleConstants.Accion.VER),
                RoleConstants.Modulo.HISTORIA_CLINICA, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR),
                RoleConstants.Modulo.LABORATORIOS, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.ORDENAR),
                RoleConstants.Modulo.IMAGENES, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.ORDENAR),
                RoleConstants.Modulo.URGENCIAS, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR),
                RoleConstants.Modulo.HOSPITALIZACION, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR),
                RoleConstants.Modulo.FARMACIA, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.PRESCRIBIR),
                RoleConstants.Modulo.CITAS, Set.of(RoleConstants.Accion.VER)
        )));

        // ODONTOLOGO: similar a médico para su ámbito
        m.put(RoleConstants.ODONTOLOGO, new HashMap<>(Map.of(
                RoleConstants.Modulo.DASHBOARD, Set.of(RoleConstants.Accion.VER),
                RoleConstants.Modulo.PACIENTES, Set.of(RoleConstants.Accion.VER),
                RoleConstants.Modulo.HISTORIA_CLINICA, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR),
                RoleConstants.Modulo.LABORATORIOS, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.ORDENAR),
                RoleConstants.Modulo.IMAGENES, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.ORDENAR),
                RoleConstants.Modulo.URGENCIAS, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR),
                RoleConstants.Modulo.HOSPITALIZACION, Set.of(RoleConstants.Accion.VER),
                RoleConstants.Modulo.FARMACIA, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.PRESCRIBIR),
                RoleConstants.Modulo.CITAS, Set.of(RoleConstants.Accion.VER)
        )));

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

        // JEFE_ENFERMERIA: todo lo de enfermería + más
        m.put(RoleConstants.JEFE_ENFERMERIA, new HashMap<>(Map.of(
                RoleConstants.Modulo.PACIENTES, Set.of(RoleConstants.Accion.VER),
                RoleConstants.Modulo.HISTORIA_CLINICA, Set.of(RoleConstants.Accion.VER),
                RoleConstants.Modulo.URGENCIAS, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR),
                RoleConstants.Modulo.HOSPITALIZACION, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR, RoleConstants.Accion.ELIMINAR),
                RoleConstants.Modulo.CITAS, Set.of(RoleConstants.Accion.VER),
                RoleConstants.Modulo.DASHBOARD, Set.of(RoleConstants.Accion.VER)
        )));

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

        // RECEPCIONISTA
        m.put(RoleConstants.RECEPCIONISTA, new HashMap<>(Map.of(
                RoleConstants.Modulo.PACIENTES, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR),
                RoleConstants.Modulo.CITAS, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.CREAR, RoleConstants.Accion.EDITAR, RoleConstants.Accion.ELIMINAR),
                RoleConstants.Modulo.FACTURACION, Set.of(RoleConstants.Accion.VER, RoleConstants.Accion.FACTURAR),
                RoleConstants.Modulo.DASHBOARD, Set.of(RoleConstants.Accion.VER)
        )));
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
