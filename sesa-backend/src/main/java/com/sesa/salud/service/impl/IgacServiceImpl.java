/**
 * Implementación servicio IGAC – catálogo límites oficiales.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.IgacDepartamentoDto;
import com.sesa.salud.dto.IgacMunicipioDto;
import com.sesa.salud.dto.IgacVeredaDto;
import com.sesa.salud.entity.master.IgacDepartamento;
import com.sesa.salud.entity.master.IgacMunicipio;
import com.sesa.salud.entity.master.IgacVereda;
import com.sesa.salud.repository.master.IgacDepartamentoRepository;
import com.sesa.salud.repository.master.IgacMunicipioRepository;
import com.sesa.salud.repository.master.IgacVeredaRepository;
import com.sesa.salud.service.IgacService;
import com.sesa.salud.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IgacServiceImpl implements IgacService {

    private static final String PUBLIC = "public";

    private final IgacDepartamentoRepository departamentoRepository;
    private final IgacMunicipioRepository municipioRepository;
    private final IgacVeredaRepository veredaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<IgacDepartamentoDto> listDepartamentos() {
        String prev = TenantContextHolder.getTenantSchema();
        try {
            TenantContextHolder.setTenantSchema(PUBLIC);
            return departamentoRepository.findAllByOrderByNombreAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        } finally {
            TenantContextHolder.setTenantSchema(prev != null ? prev : "public");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<IgacMunicipioDto> listMunicipiosPorDepartamento(String departamentoCodigo) {
        String prev = TenantContextHolder.getTenantSchema();
        try {
            TenantContextHolder.setTenantSchema(PUBLIC);
            return municipioRepository.findByDepartamentoCodigoOrderByNombreAsc(departamentoCodigo).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        } finally {
            TenantContextHolder.setTenantSchema(prev != null ? prev : "public");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<IgacVeredaDto> listVeredasPorMunicipio(String municipioCodigo) {
        String prev = TenantContextHolder.getTenantSchema();
        try {
            TenantContextHolder.setTenantSchema(PUBLIC);
            return veredaRepository.findByMunicipioCodigoOrderByNombreAsc(municipioCodigo).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        } finally {
            TenantContextHolder.setTenantSchema(prev != null ? prev : "public");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IgacDepartamentoDto> getDepartamentoByCodigo(String codigoDane) {
        String prev = TenantContextHolder.getTenantSchema();
        try {
            TenantContextHolder.setTenantSchema(PUBLIC);
            return departamentoRepository.findByCodigoDane(codigoDane).map(this::toDto);
        } finally {
            TenantContextHolder.setTenantSchema(prev != null ? prev : "public");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IgacMunicipioDto> getMunicipioByCodigo(String codigoDane) {
        String prev = TenantContextHolder.getTenantSchema();
        try {
            TenantContextHolder.setTenantSchema(PUBLIC);
            return municipioRepository.findByCodigoDane(codigoDane).map(this::toDto);
        } finally {
            TenantContextHolder.setTenantSchema(prev != null ? prev : "public");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IgacVeredaDto> getVeredaByCodigo(String codigo) {
        String prev = TenantContextHolder.getTenantSchema();
        try {
            TenantContextHolder.setTenantSchema(PUBLIC);
            return veredaRepository.findByCodigo(codigo).map(this::toDto);
        } finally {
            TenantContextHolder.setTenantSchema(prev != null ? prev : "public");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getVeredaGeojson(String codigo) {
        String prev = TenantContextHolder.getTenantSchema();
        try {
            TenantContextHolder.setTenantSchema(PUBLIC);
            return veredaRepository.findByCodigo(codigo)
                .map(IgacVereda::getGeometryJson)
                .filter(j -> j != null && !j.isBlank());
        } finally {
            TenantContextHolder.setTenantSchema(prev != null ? prev : "public");
        }
    }

    private IgacDepartamentoDto toDto(IgacDepartamento e) {
        return IgacDepartamentoDto.builder()
            .id(e.getId())
            .codigoDane(e.getCodigoDane())
            .nombre(e.getNombre())
            .build();
    }

    private IgacMunicipioDto toDto(IgacMunicipio e) {
        return IgacMunicipioDto.builder()
            .id(e.getId())
            .codigoDane(e.getCodigoDane())
            .departamentoCodigo(e.getDepartamentoCodigo())
            .nombre(e.getNombre())
            .build();
    }

    private IgacVeredaDto toDto(IgacVereda e) {
        return IgacVeredaDto.builder()
            .id(e.getId())
            .codigo(e.getCodigo())
            .municipioCodigo(e.getMunicipioCodigo())
            .nombre(e.getNombre())
            .geometryJson(e.getGeometryJson())
            .build();
    }
}
