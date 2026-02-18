/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.LogoResourceDto;
import com.sesa.salud.dto.PersonalDto;
import com.sesa.salud.dto.PersonalRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface PersonalService {

    Page<PersonalDto> findAll(Pageable pageable);

    Page<PersonalDto> search(String q, Pageable pageable);

    PersonalDto findById(Long id);

    PersonalDto create(PersonalRequestDto dto);

    PersonalDto update(Long id, PersonalRequestDto dto);

    void deleteById(Long id);

    void saveFoto(Long id, MultipartFile file);

    void saveFirma(Long id, MultipartFile file);

    Optional<LogoResourceDto> getFotoResource(Long id);

    Optional<LogoResourceDto> getFirmaResource(Long id);
}
