/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository.master;

import com.sesa.salud.entity.master.ArchivoAlmacenamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArchivoAlmacenamientoRepository extends JpaRepository<ArchivoAlmacenamiento, String> {

    Optional<ArchivoAlmacenamiento> findByUuid(String uuid);

    /** Busca el UUID del archivo más reciente asociado a una entidad y su ID. */
    Optional<ArchivoAlmacenamiento> findTopByEntidadAndEntidadIdOrderByCreatedAtDesc(
            String entidad, String entidadId);
}
