/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.entity.master.Modulo;
import com.sesa.salud.entity.master.Submodulo;
import com.sesa.salud.repository.master.ModuloRepository;
import com.sesa.salud.repository.master.SubmoduloRepository;
import com.sesa.salud.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/modulos")
@RequiredArgsConstructor
public class ModuloController {

    private final ModuloRepository moduloRepository;
    private final SubmoduloRepository submoduloRepository;

    /** Devuelve módulos con sus submódulos anidados. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMINISTRADOR')")
    public ResponseEntity<List<Map<String, Object>>> list() {
        TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
        List<Modulo> modulos = moduloRepository.findAllByOrderByNombreAsc();
        List<Submodulo> allSubs = submoduloRepository.findAllByOrderByModuloIdAscNombreAsc();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Modulo m : modulos) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("codigo", m.getCodigo());
            map.put("nombre", m.getNombre());
            List<Map<String, Object>> subs = new ArrayList<>();
            for (Submodulo s : allSubs) {
                if (s.getModulo().getId().equals(m.getId())) {
                    Map<String, Object> sm = new LinkedHashMap<>();
                    sm.put("id", s.getId());
                    sm.put("codigo", s.getCodigo());
                    sm.put("nombre", s.getNombre());
                    subs.add(sm);
                }
            }
            map.put("submodulos", subs);
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }
}
