/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository.master;

import com.sesa.salud.entity.master.EmpresaModulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmpresaModuloRepository extends JpaRepository<EmpresaModulo, EmpresaModulo.EmpresaModuloId> {

    List<EmpresaModulo> findByEmpresaId(Long empresaId);

    void deleteByEmpresaId(Long empresaId);
}
