/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.LogoResourceDto;
import com.sesa.salud.dto.PersonalDto;
import com.sesa.salud.dto.PersonalRequestDto;
import com.sesa.salud.entity.Personal;
import com.sesa.salud.entity.Usuario;
import com.sesa.salud.entity.master.TenantUsuarioLogin;
import com.sesa.salud.repository.PersonalRepository;
import com.sesa.salud.repository.UsuarioRepository;
import com.sesa.salud.repository.master.TenantUsuarioLoginRepository;
import com.sesa.salud.service.PersonalService;
import com.sesa.salud.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalServiceImpl implements PersonalService {

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = List.of("png", "jpg", "jpeg", "webp");

    private final PersonalRepository personalRepository;
    private final UsuarioRepository usuarioRepository;
    private final TenantUsuarioLoginRepository tenantUsuarioLoginRepository;
    private final PasswordEncoder passwordEncoder;

    private static final java.util.Map<String, String> EXT_TO_MIME = java.util.Map.of(
            "png", "image/png", "jpg", "image/jpeg", "jpeg", "image/jpeg",
            "webp", "image/webp", "svg", "image/svg+xml");

    @Override
    @Transactional(readOnly = true)
    public Page<PersonalDto> findAll(Pageable pageable) {
        return personalRepository.findByActivoTrue(pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PersonalDto> search(String q, Pageable pageable) {
        if (q == null || q.isBlank()) {
            return findAll(pageable);
        }
        String t = q.trim();
        return personalRepository.findByNombresContainingIgnoreCaseOrApellidosContainingIgnoreCaseOrCargoContainingIgnoreCase(
                t, t, t, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PersonalDto findById(Long id) {
        Personal p = personalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Personal no encontrado: " + id));
        return toDto(p);
    }

    @Override
    @Transactional
    public PersonalDto create(PersonalRequestDto dto) {
        String schema = TenantContextHolder.getTenantSchema();
        if (TenantContextHolder.PUBLIC.equals(schema)) {
            throw new IllegalArgumentException("No se puede crear personal en el esquema public");
        }
        String email = dto.getEmail() != null ? dto.getEmail().trim() : null;
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El correo electrónico es obligatorio para crear personal con acceso");
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese correo en esta empresa");
        }
        String password = dto.getPassword() != null ? dto.getPassword() : "";
        if (password.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria para el acceso del profesional");
        }
        String nombreCompleto = buildNombreCompleto(dto);
        String rol = dto.getRol() != null && !dto.getRol().isBlank() ? dto.getRol().trim() : "USER";
        Usuario usuario = Usuario.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .nombreCompleto(nombreCompleto)
                .activo(true)
                .roles(Set.of(rol))
                .build();
        usuario = usuarioRepository.save(usuario);
        TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
        try {
            tenantUsuarioLoginRepository.save(TenantUsuarioLogin.builder()
                    .email(usuario.getEmail())
                    .schemaName(schema)
                    .build());
        } finally {
            TenantContextHolder.setTenantSchema(schema);
        }
        Personal p = Personal.builder()
                .nombres(dto.getNombres())
                .apellidos(dto.getApellidos())
                .cargo(dto.getCargo())
                .servicio(dto.getServicio())
                .turno(dto.getTurno())
                .identificacion(dto.getIdentificacion())
                .primerNombre(dto.getPrimerNombre())
                .segundoNombre(dto.getSegundoNombre())
                .primerApellido(dto.getPrimerApellido())
                .segundoApellido(dto.getSegundoApellido())
                .celular(dto.getCelular())
                .email(email)
                .rol(rol)
                .institucionPrestadora(dto.getInstitucionPrestadora())
                .activo(dto.getActivo() != null ? dto.getActivo() : true)
                .usuario(usuario)
                .build();
        p = personalRepository.save(p);
        return toDto(p);
    }

    private static String buildNombreCompleto(PersonalRequestDto dto) {
        StringBuilder sb = new StringBuilder();
        if (dto.getPrimerNombre() != null && !dto.getPrimerNombre().isBlank()) {
            sb.append(dto.getPrimerNombre());
            if (dto.getSegundoNombre() != null && !dto.getSegundoNombre().isBlank()) {
                sb.append(" ").append(dto.getSegundoNombre());
            }
        }
        if (dto.getPrimerApellido() != null && !dto.getPrimerApellido().isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(dto.getPrimerApellido());
            if (dto.getSegundoApellido() != null && !dto.getSegundoApellido().isBlank()) {
                sb.append(" ").append(dto.getSegundoApellido());
            }
        }
        if (sb.length() == 0 && dto.getNombres() != null) {
            sb.append(dto.getNombres());
            if (dto.getApellidos() != null && !dto.getApellidos().isBlank()) {
                sb.append(" ").append(dto.getApellidos());
            }
        }
        return sb.toString().trim();
    }

    @Override
    @Transactional
    public PersonalDto update(Long id, PersonalRequestDto dto) {
        Personal p = personalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Personal no encontrado: " + id));
        p.setNombres(dto.getNombres());
        p.setApellidos(dto.getApellidos());
        p.setCargo(dto.getCargo());
        p.setServicio(dto.getServicio());
        p.setTurno(dto.getTurno());
        p.setIdentificacion(dto.getIdentificacion());
        p.setPrimerNombre(dto.getPrimerNombre());
        p.setSegundoNombre(dto.getSegundoNombre());
        p.setPrimerApellido(dto.getPrimerApellido());
        p.setSegundoApellido(dto.getSegundoApellido());
        p.setCelular(dto.getCelular());
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            p.setEmail(dto.getEmail().trim());
            if (p.getUsuario() != null) {
                p.getUsuario().setEmail(p.getEmail());
                p.getUsuario().setNombreCompleto(buildNombreCompleto(dto));
                usuarioRepository.save(p.getUsuario());
            }
        }
        if (dto.getRol() != null && !dto.getRol().isBlank()) {
            p.setRol(dto.getRol().trim());
            if (p.getUsuario() != null) {
                p.getUsuario().getRoles().clear();
                p.getUsuario().getRoles().add(dto.getRol().trim());
                usuarioRepository.save(p.getUsuario());
            }
        }
        p.setInstitucionPrestadora(dto.getInstitucionPrestadora());
        p.setActivo(dto.getActivo());
        p = personalRepository.save(p);
        return toDto(p);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (!personalRepository.existsById(id)) {
            throw new RuntimeException("Personal no encontrado: " + id);
        }
        personalRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void saveFoto(Long id, MultipartFile file) {
        savePersonalFileToDb(id, file, true);
    }

    @Override
    @Transactional
    public void saveFirma(Long id, MultipartFile file) {
        savePersonalFileToDb(id, file, false);
    }

    private void savePersonalFileToDb(Long id, MultipartFile file, boolean isFoto) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo es obligatorio");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Nombre de archivo no válido");
        }
        int lastDot = originalFilename.lastIndexOf('.');
        List<String> allowedExt = isFoto ? ALLOWED_IMAGE_EXTENSIONS : List.of("png", "jpg", "jpeg", "webp", "svg");
        String ext = (lastDot >= 0 && lastDot < originalFilename.length() - 1)
                ? originalFilename.substring(lastDot + 1).toLowerCase() : null;
        if (ext == null || !allowedExt.contains(ext)) {
            throw new IllegalArgumentException("Formato no permitido. Use: " + String.join(", ", allowedExt));
        }
        if (TenantContextHolder.PUBLIC.equals(TenantContextHolder.getTenantSchema())) {
            throw new IllegalArgumentException("No se puede subir archivos en el esquema public");
        }
        Personal p = personalRepository.findById(id).orElseThrow(() -> new RuntimeException("Personal no encontrado: " + id));
        try {
            byte[] data = file.getBytes();
            String contentType = EXT_TO_MIME.getOrDefault(ext, "image/" + ext);
            if (isFoto) {
                p.setFotoData(data);
                p.setFotoContentType(contentType);
                p.setFotoUrl("db");
            } else {
                p.setFirmaData(data);
                p.setFirmaContentType(contentType);
                p.setFirmaUrl("db");
            }
            personalRepository.save(p);
        } catch (java.io.IOException e) {
            log.error("Error leyendo archivo", e);
            throw new RuntimeException("No se pudo guardar el archivo: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LogoResourceDto> getFotoResource(Long id) {
        return getPersonalFileFromDb(id, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LogoResourceDto> getFirmaResource(Long id) {
        return getPersonalFileFromDb(id, false);
    }

    private Optional<LogoResourceDto> getPersonalFileFromDb(Long id, boolean isFoto) {
        if (TenantContextHolder.PUBLIC.equals(TenantContextHolder.getTenantSchema())) {
            return Optional.empty();
        }
        Personal p = personalRepository.findById(id).orElse(null);
        if (p == null) return Optional.empty();
        byte[] data = isFoto ? p.getFotoData() : p.getFirmaData();
        String contentType = isFoto ? p.getFotoContentType() : p.getFirmaContentType();
        if (data == null || data.length == 0) return Optional.empty();
        return Optional.of(new LogoResourceDto(
                new ByteArrayResource(data),
                contentType != null ? contentType : "image/png"));
    }

    private PersonalDto toDto(Personal p) {
        return PersonalDto.builder()
                .id(p.getId())
                .nombres(p.getNombres())
                .apellidos(p.getApellidos())
                .cargo(p.getCargo())
                .servicio(p.getServicio())
                .turno(p.getTurno())
                .identificacion(p.getIdentificacion())
                .primerNombre(p.getPrimerNombre())
                .segundoNombre(p.getSegundoNombre())
                .primerApellido(p.getPrimerApellido())
                .segundoApellido(p.getSegundoApellido())
                .celular(p.getCelular())
                .email(p.getEmail())
                .rol(p.getRol())
                .institucionPrestadora(p.getInstitucionPrestadora())
                .fotoUrl(p.getFotoUrl())
                .firmaUrl(p.getFirmaUrl())
                .activo(p.getActivo())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
