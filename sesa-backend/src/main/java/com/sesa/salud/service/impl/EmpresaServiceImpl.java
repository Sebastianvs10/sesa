/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.EmpresaCreateRequest;
import com.sesa.salud.dto.EmpresaDto;
import com.sesa.salud.dto.LogoResourceDto;
import com.sesa.salud.event.email.TenantAdminWelcomeEmailEvent;
import com.sesa.salud.entity.master.Empresa;
import com.sesa.salud.entity.master.EmpresaModulo;
import com.sesa.salud.entity.master.EmpresaSubmodulo;
import com.sesa.salud.entity.master.Modulo;
import com.sesa.salud.entity.master.Submodulo;
import com.sesa.salud.entity.master.TenantUsuarioLogin;
import com.sesa.salud.repository.master.EmpresaModuloRepository;
import com.sesa.salud.repository.master.EmpresaRepository;
import com.sesa.salud.repository.master.EmpresaSubmoduloRepository;
import com.sesa.salud.repository.master.ModuloRepository;
import com.sesa.salud.repository.master.SubmoduloRepository;
import com.sesa.salud.repository.master.TenantUsuarioLoginRepository;
import com.sesa.salud.service.ArchivoService;
import com.sesa.salud.service.EmpresaService;
import com.sesa.salud.service.TenantSetupService;
import com.sesa.salud.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmpresaServiceImpl implements EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final ModuloRepository moduloRepository;
    private final SubmoduloRepository submoduloRepository;
    private final EmpresaModuloRepository empresaModuloRepository;
    private final EmpresaSubmoduloRepository empresaSubmoduloRepository;
    private final TenantUsuarioLoginRepository tenantUsuarioLoginRepository;
    private final TenantSetupService tenantSetupService;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final ArchivoService archivoService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public EmpresaDto create(EmpresaCreateRequest request) {
        String schemaName = sanitizeSchemaName(request.getSchemaName());
        if (schemaName.isEmpty()) {
            throw new IllegalArgumentException("Schema no válido");
        }
        TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
        if (empresaRepository.existsBySchemaName(schemaName)) {
            throw new IllegalArgumentException("Ya existe una empresa con el schema: " + schemaName);
        }
        if (request.getAdminUser().getCorreo() != null && tenantUsuarioLoginRepository.existsByEmail(request.getAdminUser().getCorreo().trim())) {
            throw new IllegalArgumentException("El correo del administrador ya está registrado en otra empresa");
        }

        Connection conn = DataSourceUtils.getConnection(dataSource);
        try {
            conn.createStatement().execute("CREATE SCHEMA IF NOT EXISTS \"" + schemaName + "\"");
        } catch (Exception e) {
            log.error("Error creando schema {}", schemaName, e);
            throw new RuntimeException("No se pudo crear el esquema: " + e.getMessage());
        }
        // No cerrar conn: es la de la transacción

        String adminEmail = tenantSetupService.createTenantSchemaAndAdmin(schemaName, request);

        TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
        jdbcTemplate.execute("SET search_path TO public");
        tenantUsuarioLoginRepository.save(TenantUsuarioLogin.builder()
                .email(adminEmail)
                .schemaName(schemaName)
                .build());

        EmpresaCreateRequest.AdminUserRequest admin = request.getAdminUser();
        Empresa empresa = Empresa.builder()
                .schemaName(schemaName)
                .adminEmail(admin.getCorreo() != null ? admin.getCorreo().trim() : null)
                .adminIdentificacion(admin.getIdentificacion() != null ? admin.getIdentificacion().trim() : null)
                .adminPrimerNombre(admin.getPrimerNombre() != null ? admin.getPrimerNombre().trim() : null)
                .adminSegundoNombre(admin.getSegundoNombre() != null && !admin.getSegundoNombre().isBlank() ? admin.getSegundoNombre().trim() : null)
                .adminPrimerApellido(admin.getPrimerApellido() != null ? admin.getPrimerApellido().trim() : null)
                .adminSegundoApellido(admin.getSegundoApellido() != null && !admin.getSegundoApellido().isBlank() ? admin.getSegundoApellido().trim() : null)
                .adminCelular(admin.getTelefonoCelular() != null ? admin.getTelefonoCelular().trim() : null)
                .razonSocial(request.getRazonSocial())
                .telefono(request.getTelefono())
                .segundoTelefono(request.getSegundoTelefono())
                .identificacion(request.getIdentificacion())
                .direccionEmpresa(request.getDireccionEmpresa())
                .tipoDocumento(request.getTipoDocumento())
                .regimen(request.getRegimen())
                .numeroDivipola(request.getNumeroDivipola())
                .pais(request.getPais())
                .departamento(request.getDepartamento())
                .municipio(request.getMunicipio())
                .imagenUrl(request.getImagenUrl())
                .usuarioMovilLimit(request.getUsuarioMovilLimit() != null ? request.getUsuarioMovilLimit() : 0)
                .usuarioWebLimit(request.getUsuarioWebLimit() != null ? request.getUsuarioWebLimit() : 0)
                .activo(true)
                .build();
        empresa = empresaRepository.save(empresa);

        saveModulosAndSubmodulos(empresa.getId(), request);

        String adminNombre = buildAdminDisplayName(request.getAdminUser());
        eventPublisher.publishEvent(new TenantAdminWelcomeEmailEvent(
                adminEmail, adminNombre, request.getRazonSocial()));

        return toDto(empresa);
    }

    private static String buildAdminDisplayName(EmpresaCreateRequest.AdminUserRequest admin) {
        if (admin == null) return "";
        StringBuilder sb = new StringBuilder();
        if (admin.getPrimerNombre() != null) sb.append(admin.getPrimerNombre());
        if (admin.getSegundoNombre() != null && !admin.getSegundoNombre().isBlank()) {
            sb.append(" ").append(admin.getSegundoNombre());
        }
        if (admin.getPrimerApellido() != null) sb.append(" ").append(admin.getPrimerApellido());
        if (admin.getSegundoApellido() != null && !admin.getSegundoApellido().isBlank()) {
            sb.append(" ").append(admin.getSegundoApellido());
        }
        return sb.toString().trim();
    }

    private static String sanitizeSchemaName(String name) {
        if (name == null) return "";
        return name.trim().toLowerCase().replaceAll("[^a-z0-9_]", "_").replaceAll("_+", "_");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmpresaDto> findAll(Pageable pageable) {
        TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
        return empresaRepository.findAll(pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmpresaDto> findBySchemaName(String schemaName) {
        if (schemaName == null || schemaName.isBlank() || TenantContextHolder.PUBLIC.equals(schemaName)) {
            return Optional.empty();
        }
        TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
        return empresaRepository.findBySchemaName(schemaName).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public EmpresaDto findById(Long id) {
        TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
        Empresa e = empresaRepository.findById(id).orElseThrow(() -> new RuntimeException("Empresa no encontrada: " + id));
        EmpresaDto dto = toDto(e);
        List<Long> modIds = empresaModuloRepository.findByEmpresaId(id).stream()
                .map(EmpresaModulo::getModuloId)
                .toList();
        if (!modIds.isEmpty()) {
            dto.setModuloCodigos(moduloRepository.findAllById(modIds).stream()
                    .map(Modulo::getCodigo)
                    .collect(Collectors.toList()));
        }
        List<Long> subIds = empresaSubmoduloRepository.findByEmpresaId(id).stream()
                .map(EmpresaSubmodulo::getSubmoduloId)
                .toList();
        if (!subIds.isEmpty()) {
            dto.setSubmoduloCodigos(submoduloRepository.findAllById(subIds).stream()
                    .map(Submodulo::getCodigo)
                    .collect(Collectors.toList()));
        }
        if (dto.getAdminCorreo() == null && e.getSchemaName() != null) {
            tenantUsuarioLoginRepository.findBySchemaName(e.getSchemaName()).stream()
                    .findFirst()
                    .map(TenantUsuarioLogin::getEmail)
                    .ifPresent(dto::setAdminCorreo);
        }
        return dto;
    }

    @Override
    @Transactional
    public EmpresaDto update(Long id, EmpresaCreateRequest request) {
        TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
        Empresa e = empresaRepository.findById(id).orElseThrow(() -> new RuntimeException("Empresa no encontrada: " + id));
        EmpresaCreateRequest.AdminUserRequest admin = request.getAdminUser();
        if (admin != null) {
            e.setAdminIdentificacion(admin.getIdentificacion() != null ? admin.getIdentificacion().trim() : null);
            e.setAdminPrimerNombre(admin.getPrimerNombre() != null ? admin.getPrimerNombre().trim() : null);
            e.setAdminSegundoNombre(admin.getSegundoNombre() != null && !admin.getSegundoNombre().isBlank() ? admin.getSegundoNombre().trim() : null);
            e.setAdminPrimerApellido(admin.getPrimerApellido() != null ? admin.getPrimerApellido().trim() : null);
            e.setAdminSegundoApellido(admin.getSegundoApellido() != null && !admin.getSegundoApellido().isBlank() ? admin.getSegundoApellido().trim() : null);
            e.setAdminCelular(admin.getTelefonoCelular() != null ? admin.getTelefonoCelular().trim() : null);
        }
        e.setRazonSocial(request.getRazonSocial());
        e.setTelefono(request.getTelefono());
        e.setSegundoTelefono(request.getSegundoTelefono());
        e.setIdentificacion(request.getIdentificacion());
        e.setDireccionEmpresa(request.getDireccionEmpresa());
        e.setTipoDocumento(request.getTipoDocumento());
        e.setRegimen(request.getRegimen());
        e.setNumeroDivipola(request.getNumeroDivipola());
        e.setPais(request.getPais());
        e.setDepartamento(request.getDepartamento());
        e.setMunicipio(request.getMunicipio());
        e.setImagenUrl(request.getImagenUrl());
        e.setUsuarioMovilLimit(request.getUsuarioMovilLimit() != null ? request.getUsuarioMovilLimit() : 0);
        e.setUsuarioWebLimit(request.getUsuarioWebLimit() != null ? request.getUsuarioWebLimit() : 0);
        empresaModuloRepository.deleteByEmpresaId(id);
        empresaSubmoduloRepository.deleteByEmpresaId(id);
        saveModulosAndSubmodulos(e.getId(), request);
        return toDto(empresaRepository.save(e));
    }

    private void saveModulosAndSubmodulos(Long empresaId, EmpresaCreateRequest request) {
        if (request.getModuloCodigos() != null && !request.getModuloCodigos().isEmpty()) {
            List<Modulo> modulos = moduloRepository.findAll().stream()
                    .filter(m -> request.getModuloCodigos().contains(m.getCodigo()))
                    .toList();
            for (Modulo m : modulos) {
                empresaModuloRepository.save(EmpresaModulo.builder()
                        .empresaId(empresaId)
                        .moduloId(m.getId())
                        .build());
            }
        }
        if (request.getSubmoduloCodigos() != null && !request.getSubmoduloCodigos().isEmpty()) {
            List<Submodulo> submodulos = submoduloRepository.findAll().stream()
                    .filter(s -> request.getSubmoduloCodigos().contains(s.getCodigo()))
                    .toList();
            for (Submodulo s : submodulos) {
                empresaSubmoduloRepository.save(EmpresaSubmodulo.builder()
                        .empresaId(empresaId)
                        .submoduloId(s.getId())
                        .build());
            }
        }
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
        if (!empresaRepository.existsById(id)) {
            throw new RuntimeException("Empresa no encontrada: " + id);
        }
        empresaSubmoduloRepository.deleteByEmpresaId(id);
        empresaModuloRepository.deleteByEmpresaId(id);
        empresaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public String saveLogo(String schemaName, MultipartFile file) {
        TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
        Empresa emp = empresaRepository.findBySchemaName(schemaName)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada para el tenant: " + schemaName));
        String uuid = archivoService.guardar(file, "EMPRESA_LOGO",
                emp.getId().toString(), TenantContextHolder.PUBLIC, true);
        emp.setImagenUrl(uuid);
        emp.setImagenData(null);
        emp.setImagenContentType(null);
        empresaRepository.save(emp);
        return uuid;
    }

    @Override
    @Transactional
    public String saveLogoById(Long empresaId, MultipartFile file) {
        TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
        Empresa emp = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada con id: " + empresaId));
        String uuid = archivoService.guardar(file, "EMPRESA_LOGO",
                emp.getId().toString(), TenantContextHolder.PUBLIC, true);
        emp.setImagenUrl(uuid);
        emp.setImagenData(null);
        emp.setImagenContentType(null);
        empresaRepository.save(emp);
        return uuid;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LogoResourceDto> getLogoResource(String schemaName) {
        if (schemaName == null || schemaName.isBlank() || TenantContextHolder.PUBLIC.equals(schemaName)) {
            return Optional.empty();
        }
        TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
        return empresaRepository.findBySchemaName(schemaName)
                .flatMap(e -> {
                    // Nuevo sistema: imagenUrl contiene un UUID
                    if (e.getImagenUrl() != null && isUuid(e.getImagenUrl())) {
                        return archivoService.obtener(e.getImagenUrl())
                                .map(r -> new LogoResourceDto(
                                        new ByteArrayResource(r.datos()),
                                        r.contentType()));
                    }
                    // Sistema legacy: datos binarios en imagenData
                    if (e.getImagenData() != null && e.getImagenData().length > 0) {
                        String ct = e.getImagenContentType() != null ? e.getImagenContentType() : "image/png";
                        return Optional.of(new LogoResourceDto(new ByteArrayResource(e.getImagenData()), ct));
                    }
                    return Optional.empty();
                });
    }

    /** Verifica si un string tiene formato UUID v4 estándar. */
    private static boolean isUuid(String s) {
        return s != null && s.matches(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    }

    private EmpresaDto toDto(Empresa e) {
        return EmpresaDto.builder()
                .id(e.getId())
                .schemaName(e.getSchemaName())
                .adminCorreo(e.getAdminEmail())
                .adminIdentificacion(e.getAdminIdentificacion())
                .adminPrimerNombre(e.getAdminPrimerNombre())
                .adminSegundoNombre(e.getAdminSegundoNombre())
                .adminPrimerApellido(e.getAdminPrimerApellido())
                .adminSegundoApellido(e.getAdminSegundoApellido())
                .adminCelular(e.getAdminCelular())
                .razonSocial(e.getRazonSocial())
                .telefono(e.getTelefono())
                .segundoTelefono(e.getSegundoTelefono())
                .identificacion(e.getIdentificacion())
                .direccionEmpresa(e.getDireccionEmpresa())
                .tipoDocumento(e.getTipoDocumento())
                .regimen(e.getRegimen())
                .numeroDivipola(e.getNumeroDivipola())
                .pais(e.getPais())
                .departamento(e.getDepartamento())
                .municipio(e.getMunicipio())
                .imagenUrl(e.getImagenUrl())
                .usuarioMovilLimit(e.getUsuarioMovilLimit())
                .usuarioWebLimit(e.getUsuarioWebLimit())
                .activo(e.getActivo())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
