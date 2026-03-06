/**
 * Implementación de configuración de facturación electrónica DIAN por tenant.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.FacturacionElectronicaConfigDto;
import com.sesa.salud.entity.FacturacionElectronicaConfig;
import com.sesa.salud.repository.FacturacionElectronicaConfigRepository;
import com.sesa.salud.service.FacturacionElectronicaConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FacturacionElectronicaConfigServiceImpl implements FacturacionElectronicaConfigService {

    private final FacturacionElectronicaConfigRepository repository;

    @Override
    @Transactional(readOnly = true)
    public FacturacionElectronicaConfigDto getOrCreate() {
        FacturacionElectronicaConfig config = repository.findTopByOrderByIdAsc()
                .orElseGet(() -> repository.save(FacturacionElectronicaConfig.builder().build()));
        return toDto(config);
    }

    @Override
    @Transactional
    public FacturacionElectronicaConfigDto update(FacturacionElectronicaConfigDto dto) {
        FacturacionElectronicaConfig config = repository.findTopByOrderByIdAsc()
                .orElseGet(() -> FacturacionElectronicaConfig.builder().build());

        if (dto.getFacturacionActiva() != null) config.setFacturacionActiva(dto.getFacturacionActiva());
        if (dto.getNit() != null) config.setNit(dto.getNit());
        if (dto.getRazonSocial() != null) config.setRazonSocial(dto.getRazonSocial());
        if (dto.getNombreComercial() != null) config.setNombreComercial(dto.getNombreComercial());
        if (dto.getRegimen() != null) config.setRegimen(dto.getRegimen());
        if (dto.getDireccion() != null) config.setDireccion(dto.getDireccion());
        if (dto.getMunicipio() != null) config.setMunicipio(dto.getMunicipio());
        if (dto.getDepartamento() != null) config.setDepartamento(dto.getDepartamento());
        if (dto.getPais() != null) config.setPais(dto.getPais());
        if (dto.getEmailContacto() != null) config.setEmailContacto(dto.getEmailContacto());
        if (dto.getAmbiente() != null) config.setAmbiente(dto.getAmbiente());
        if (dto.getNumeroResolucion() != null) config.setNumeroResolucion(dto.getNumeroResolucion());
        if (dto.getFechaResolucion() != null) config.setFechaResolucion(dto.getFechaResolucion());
        if (dto.getPrefijo() != null) config.setPrefijo(dto.getPrefijo());
        if (dto.getRangoDesde() != null) config.setRangoDesde(dto.getRangoDesde());
        if (dto.getRangoHasta() != null) config.setRangoHasta(dto.getRangoHasta());
        if (dto.getClaveTecnica() != null) config.setClaveTecnica(dto.getClaveTecnica());
        if (dto.getSoftwareId() != null) config.setSoftwareId(dto.getSoftwareId());
        if (dto.getSoftwarePin() != null) config.setSoftwarePin(dto.getSoftwarePin());
        if (dto.getPlantillaPdf() != null) config.setPlantillaPdf(dto.getPlantillaPdf());

        FacturacionElectronicaConfig saved = repository.save(config);
        return toDto(saved);
    }

    private static FacturacionElectronicaConfigDto toDto(FacturacionElectronicaConfig c) {
        return FacturacionElectronicaConfigDto.builder()
                .id(c.getId())
                .facturacionActiva(c.getFacturacionActiva())
                .nit(c.getNit())
                .razonSocial(c.getRazonSocial())
                .nombreComercial(c.getNombreComercial())
                .regimen(c.getRegimen())
                .direccion(c.getDireccion())
                .municipio(c.getMunicipio())
                .departamento(c.getDepartamento())
                .pais(c.getPais())
                .emailContacto(c.getEmailContacto())
                .ambiente(c.getAmbiente())
                .numeroResolucion(c.getNumeroResolucion())
                .fechaResolucion(c.getFechaResolucion())
                .prefijo(c.getPrefijo())
                .rangoDesde(c.getRangoDesde())
                .rangoHasta(c.getRangoHasta())
                .claveTecnica(c.getClaveTecnica())
                .softwareId(c.getSoftwareId())
                .softwarePin(c.getSoftwarePin())
                .plantillaPdf(c.getPlantillaPdf())
                .build();
    }
}

