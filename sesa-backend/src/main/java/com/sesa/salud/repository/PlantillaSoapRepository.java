/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.PlantillaSoap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlantillaSoapRepository extends JpaRepository<PlantillaSoap, Long> {

    List<PlantillaSoap> findByActivoTrueOrderByNombreAsc();
}
