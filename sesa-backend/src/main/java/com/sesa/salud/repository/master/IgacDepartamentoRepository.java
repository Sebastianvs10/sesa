/**
 * Repositorio catálogo IGAC – Departamentos.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository.master;

import com.sesa.salud.entity.master.IgacDepartamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IgacDepartamentoRepository extends JpaRepository<IgacDepartamento, Long> {

    List<IgacDepartamento> findAllByOrderByNombreAsc();

    Optional<IgacDepartamento> findByCodigoDane(String codigoDane);
}
