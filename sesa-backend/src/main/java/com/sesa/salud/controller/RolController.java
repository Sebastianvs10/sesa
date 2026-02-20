/**
 * API de gestión de roles y permisos (SUPERADMINISTRADOR).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.RolCreateRequest;
import com.sesa.salud.dto.RolModulosRequest;
import com.sesa.salud.entity.master.Role;
import com.sesa.salud.repository.master.RoleRepository;
import com.sesa.salud.security.JwtPrincipal;
import com.sesa.salud.security.RoleConstants;
import com.sesa.salud.service.PermissionService;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/roles")
public class RolController {

    private final PermissionService permissionService;
    private final RoleRepository roleRepository;

    public RolController(@Lazy PermissionService permissionService, RoleRepository roleRepository) {
        this.permissionService = permissionService;
        this.roleRepository = roleRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMINISTRADOR')")
    public ResponseEntity<List<Map<String, Object>>> listRoles() {
        List<Role> allRoles = roleRepository.findAll();
        List<Map<String, Object>> result = allRoles.stream()
                .map(r -> {
                    Set<String> roles = Set.of(r.getCodigo());
                    List<String> modulos = permissionService.getAccessibleModules(roles).stream()
                            .map(Enum::name)
                            .collect(Collectors.toList());
                    return Map.<String, Object>of(
                            "codigo", r.getCodigo(),
                            "nombre", r.getNombre(),
                            "modulos", modulos
                    );
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPERADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> createRole(@RequestBody @Valid RolCreateRequest request) {
        String codigo = request.getCodigo().trim().toUpperCase().replace(" ", "_");
        if (roleRepository.existsByCodigo(codigo)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Ya existe un rol con el código " + codigo));
        }
        Role role = Role.builder()
                .codigo(codigo)
                .nombre(request.getNombre().trim())
                .build();
        roleRepository.save(role);
        return ResponseEntity.ok(Map.of(
                "codigo", role.getCodigo(),
                "nombre", role.getNombre(),
                "modulos", List.<String>of()
        ));
    }

    @GetMapping("/permisos")
    @PreAuthorize("hasRole('SUPERADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> getPermisosMatriz() {
        Map<String, Object> matriz = Map.of(
                "roles", RoleConstants.ALL_ROLES.stream().collect(Collectors.toList()),
                "modulos", List.of(RoleConstants.Modulo.values()),
                "acciones", List.of(RoleConstants.Accion.values())
        );
        return ResponseEntity.ok(matriz);
    }

    @PutMapping("/{rol}/modulos")
    @PreAuthorize("hasRole('SUPERADMINISTRADOR')")
    public ResponseEntity<Void> updateModulos(
            @PathVariable String rol,
            @RequestBody @Valid RolModulosRequest request) {
        Set<RoleConstants.Modulo> modulos = request.getModulos().stream()
                .map(String::toUpperCase)
                .map(RoleConstants.Modulo::valueOf)
                .collect(Collectors.toSet());
        permissionService.updateModulosForRole(rol, modulos);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/usuario-actual")
    public ResponseEntity<Map<String, Object>> permisosUsuarioActual(@AuthenticationPrincipal JwtPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.ok(Map.of("roles", List.of(), "modulos", List.of()));
        }
        String role = principal.role();
        Set<String> roles = role != null ? Set.of(role) : Set.of();
        Set<String> modulos = permissionService.getAccessibleModules(roles).stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(Map.of(
                "roles", roles,
                "modulos", modulos
        ));
    }

    private static String nombreRol(String codigo) {
        return switch (codigo) {
            case "SUPERADMINISTRADOR"  -> "Super Usuario";
            case "ADMIN"               -> "Administrador del Sistema";
            case "MEDICO"              -> "Médico";
            case "ODONTOLOGO"          -> "Odontólogo/a";
            case "BACTERIOLOGO"        -> "Bacteriólogo";
            case "ENFERMERO"           -> "Enfermero/a";
            case "JEFE_ENFERMERIA"     -> "Jefe de Enfermería";
            case "AUXILIAR_ENFERMERIA" -> "Auxiliar de Enfermería";
            case "PSICOLOGO"           -> "Psicólogo";
            case "REGENTE_FARMACIA"    -> "Regente de Farmacia";
            case "RECEPCIONISTA"       -> "Recepcionista";
            case "COORDINADOR_MEDICO"  -> "Coordinador Médico";
            default -> codigo;
        };
    }
}
