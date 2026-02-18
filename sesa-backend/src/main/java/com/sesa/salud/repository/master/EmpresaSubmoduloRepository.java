/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository.master;

import com.sesa.salud.entity.master.EmpresaSubmodulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmpresaSubmoduloRepository extends JpaRepository<EmpresaSubmodulo, EmpresaSubmodulo.EmpresaSubmoduloId> {

    List<EmpresaSubmodulo> findByEmpresaId(Long empresaId);

    void deleteByEmpresaId(Long empresaId);
}
