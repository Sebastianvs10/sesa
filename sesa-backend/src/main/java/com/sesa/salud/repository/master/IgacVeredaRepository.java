/**
 * Repositorio catálogo IGAC – Veredas.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository.master;

import com.sesa.salud.entity.master.IgacVereda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IgacVeredaRepository extends JpaRepository<IgacVereda, Long> {

    List<IgacVereda> findByMunicipioCodigoOrderByNombreAsc(String municipioCodigo);

    Optional<IgacVereda> findByCodigo(String codigo);
}
