/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository.master;

import com.sesa.salud.entity.master.Submodulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmoduloRepository extends JpaRepository<Submodulo, Long> {

    List<Submodulo> findByModuloIdOrderByNombreAsc(Long moduloId);

    List<Submodulo> findAllByOrderByModuloIdAscNombreAsc();
}
