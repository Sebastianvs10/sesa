/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.ImagenClinica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImagenClinicaRepository extends JpaRepository<ImagenClinica, Long> {

    List<ImagenClinica> findByPaciente_IdOrderByCreatedAtDesc(Long pacienteId);

    List<ImagenClinica> findByPaciente_IdAndPiezaFdiOrderByCreatedAtDesc(Long pacienteId, Integer piezaFdi);
}
