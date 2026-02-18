/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.dto.EpsDto;
import com.sesa.salud.entity.Eps;
import com.sesa.salud.repository.EpsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/eps")
@RequiredArgsConstructor
public class EpsController {

    private final EpsRepository epsRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EpsDto>> list() {
        List<EpsDto> list = epsRepository.findByActivoTrue().stream()
                .map(e -> EpsDto.builder().id(e.getId()).codigo(e.getCodigo()).nombre(e.getNombre()).build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }
}
