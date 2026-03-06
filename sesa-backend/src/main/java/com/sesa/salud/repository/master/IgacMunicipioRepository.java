/**
 * Repositorio catálogo IGAC – Municipios.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository.master;

import com.sesa.salud.entity.master.IgacMunicipio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IgacMunicipioRepository extends JpaRepository<IgacMunicipio, Long> {

    List<IgacMunicipio> findByDepartamentoCodigoOrderByNombreAsc(String departamentoCodigo);

    Optional<IgacMunicipio> findByCodigoDane(String codigoDane);
}
