/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.EmpresaCreateRequest;
import com.sesa.salud.dto.EmpresaDto;
import com.sesa.salud.dto.LogoResourceDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface EmpresaService {

    EmpresaDto create(EmpresaCreateRequest request);

    Page<EmpresaDto> findAll(Pageable pageable);

    EmpresaDto findById(Long id);

    /** Empresa del tenant actual (por schema del contexto). */
    Optional<EmpresaDto> findBySchemaName(String schemaName);

    EmpresaDto update(Long id, EmpresaCreateRequest request);

    void deleteById(Long id);

    /** Guarda el logo de la empresa del tenant actual y retorna el UUID asignado. */
    String saveLogo(String schemaName, MultipartFile file);

    /** Guarda el logo de la empresa por ID (SUPERADMINISTRADOR) y retorna el UUID asignado. */
    String saveLogoById(Long empresaId, MultipartFile file);

    /** Devuelve el recurso del logo (bytes + contentType) de la empresa, o vacío si no hay logo. */
    Optional<LogoResourceDto> getLogoResource(String schemaName);
}
