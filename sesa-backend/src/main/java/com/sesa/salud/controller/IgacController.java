/**
 * API catálogo IGAC – Límites oficiales (Departamentos, Municipios, Veredas).
 * No depende de servidor público: datos servidos desde nuestra base.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.IgacDepartamentoDto;
import com.sesa.salud.dto.IgacMunicipioDto;
import com.sesa.salud.dto.IgacVeredaDto;
import com.sesa.salud.service.IgacService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/igac")
@RequiredArgsConstructor
public class IgacController {

    private static final String ROLES_EBS = "hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ENFERMERO','JEFE_ENFERMERIA','AUXILIAR_ENFERMERIA','EBS','COORDINADOR_TERRITORIAL','SUPERVISOR_APS')";

    private final IgacService igacService;

    @GetMapping("/departamentos")
    @PreAuthorize(ROLES_EBS)
    public List<IgacDepartamentoDto> listDepartamentos() {
        return igacService.listDepartamentos();
    }

    @GetMapping("/municipios")
    @PreAuthorize(ROLES_EBS)
    public List<IgacMunicipioDto> listMunicipios(
            @RequestParam("departamentoCodigo") String departamentoCodigo) {
        return igacService.listMunicipiosPorDepartamento(departamentoCodigo);
    }

    @GetMapping("/veredas")
    @PreAuthorize(ROLES_EBS)
    public List<IgacVeredaDto> listVeredas(
            @RequestParam("municipioCodigo") String municipioCodigo) {
        return igacService.listVeredasPorMunicipio(municipioCodigo);
    }

    @GetMapping("/departamentos/{codigo}")
    @PreAuthorize(ROLES_EBS)
    public ResponseEntity<IgacDepartamentoDto> getDepartamento(@PathVariable String codigo) {
        return igacService.getDepartamentoByCodigo(codigo)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/municipios/{codigo}")
    @PreAuthorize(ROLES_EBS)
    public ResponseEntity<IgacMunicipioDto> getMunicipio(@PathVariable String codigo) {
        return igacService.getMunicipioByCodigo(codigo)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/veredas/{codigo}")
    @PreAuthorize(ROLES_EBS)
    public ResponseEntity<IgacVeredaDto> getVereda(@PathVariable String codigo) {
        return igacService.getVeredaByCodigo(codigo)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/veredas/{codigo}/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(ROLES_EBS)
    public ResponseEntity<String> getVeredaGeojson(@PathVariable String codigo) {
        return igacService.getVeredaGeojson(codigo)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
