/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.PacienteDto;
import com.sesa.salud.dto.PacienteRequestDto;
import com.sesa.salud.entity.Eps;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.repository.EpsRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.service.PacienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PacienteServiceImpl implements PacienteService {

    private final PacienteRepository pacienteRepository;
    private final EpsRepository epsRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PacienteDto> findAll(Pageable pageable) {
        return pacienteRepository.findByActivoTrue(pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PacienteDto> search(String q, Pageable pageable) {
        if (q == null || q.isBlank()) {
            return findAll(pageable);
        }
        String t = q.trim();
        return pacienteRepository.findByNombresContainingIgnoreCaseOrApellidosContainingIgnoreCaseOrDocumentoContaining(
                t, t, t, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PacienteDto findById(Long id) {
        Paciente p = pacienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + id));
        return toDto(p);
    }

    @Override
    @Transactional
    public PacienteDto create(PacienteRequestDto dto) {
        if (pacienteRepository.existsByDocumento(dto.getDocumento())) {
            throw new RuntimeException("Ya existe un paciente con documento: " + dto.getDocumento());
        }
        Paciente p = toEntity(dto);
        if (dto.getEpsId() != null) {
            p.setEps(epsRepository.findById(dto.getEpsId()).orElse(null));
        }
        p = pacienteRepository.save(p);
        return toDto(p);
    }

    @Override
    @Transactional
    public PacienteDto update(Long id, PacienteRequestDto dto) {
        Paciente p = pacienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + id));
        p.setTipoDocumento(dto.getTipoDocumento());
        p.setDocumento(dto.getDocumento());
        p.setNombres(dto.getNombres());
        p.setApellidos(dto.getApellidos());
        p.setFechaNacimiento(dto.getFechaNacimiento());
        p.setSexo(dto.getSexo());
        p.setGrupoSanguineo(dto.getGrupoSanguineo());
        p.setTelefono(dto.getTelefono());
        p.setEmail(dto.getEmail());
        p.setDireccion(dto.getDireccion());
        if (dto.getEpsId() != null) {
            p.setEps(epsRepository.findById(dto.getEpsId()).orElse(null));
        } else {
            p.setEps(null);
        }
        p.setActivo(dto.getActivo());
        p.setMunicipioResidencia(dto.getMunicipioResidencia());
        p.setDepartamentoResidencia(dto.getDepartamentoResidencia());
        p.setZonaResidencia(dto.getZonaResidencia());
        p.setRegimenAfiliacion(dto.getRegimenAfiliacion());
        p.setTipoUsuario(dto.getTipoUsuario());
        p.setContactoEmergenciaNombre(dto.getContactoEmergenciaNombre());
        p.setContactoEmergenciaTelefono(dto.getContactoEmergenciaTelefono());
        p.setEstadoCivil(dto.getEstadoCivil());
        p.setEscolaridad(dto.getEscolaridad());
        p.setOcupacion(dto.getOcupacion());
        p.setPertenenciaEtnica(dto.getPertenenciaEtnica());
        p = pacienteRepository.save(p);
        return toDto(p);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (!pacienteRepository.existsById(id)) {
            throw new RuntimeException("Paciente no encontrado: " + id);
        }
        pacienteRepository.deleteById(id);
    }

    private PacienteDto toDto(Paciente p) {
        return PacienteDto.builder()
                .id(p.getId())
                .tipoDocumento(p.getTipoDocumento())
                .documento(p.getDocumento())
                .nombres(p.getNombres())
                .apellidos(p.getApellidos())
                .fechaNacimiento(p.getFechaNacimiento())
                .sexo(p.getSexo())
                .grupoSanguineo(p.getGrupoSanguineo())
                .telefono(p.getTelefono())
                .email(p.getEmail())
                .direccion(p.getDireccion())
                .epsId(p.getEps() != null ? p.getEps().getId() : null)
                .epsNombre(p.getEps() != null ? p.getEps().getNombre() : null)
                .activo(p.getActivo())
                .createdAt(p.getCreatedAt())
                .municipioResidencia(p.getMunicipioResidencia())
                .departamentoResidencia(p.getDepartamentoResidencia())
                .zonaResidencia(p.getZonaResidencia())
                .regimenAfiliacion(p.getRegimenAfiliacion())
                .tipoUsuario(p.getTipoUsuario())
                .contactoEmergenciaNombre(p.getContactoEmergenciaNombre())
                .contactoEmergenciaTelefono(p.getContactoEmergenciaTelefono())
                .estadoCivil(p.getEstadoCivil())
                .escolaridad(p.getEscolaridad())
                .ocupacion(p.getOcupacion())
                .pertenenciaEtnica(p.getPertenenciaEtnica())
                .build();
    }

    private Paciente toEntity(PacienteRequestDto dto) {
        return Paciente.builder()
                .tipoDocumento(dto.getTipoDocumento())
                .documento(dto.getDocumento())
                .nombres(dto.getNombres())
                .apellidos(dto.getApellidos())
                .fechaNacimiento(dto.getFechaNacimiento())
                .sexo(dto.getSexo())
                .grupoSanguineo(dto.getGrupoSanguineo())
                .telefono(dto.getTelefono())
                .email(dto.getEmail())
                .direccion(dto.getDireccion())
                .activo(dto.getActivo() != null ? dto.getActivo() : true)
                .municipioResidencia(dto.getMunicipioResidencia())
                .departamentoResidencia(dto.getDepartamentoResidencia())
                .zonaResidencia(dto.getZonaResidencia())
                .regimenAfiliacion(dto.getRegimenAfiliacion())
                .tipoUsuario(dto.getTipoUsuario())
                .contactoEmergenciaNombre(dto.getContactoEmergenciaNombre())
                .contactoEmergenciaTelefono(dto.getContactoEmergenciaTelefono())
                .estadoCivil(dto.getEstadoCivil())
                .escolaridad(dto.getEscolaridad())
                .ocupacion(dto.getOcupacion())
                .pertenenciaEtnica(dto.getPertenenciaEtnica())
                .build();
    }
}
