/**
 * Repositorio para permisos rol-módulo.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository.master;

import com.sesa.salud.entity.master.RoleModuloPermiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface RoleModuloPermisoRepository extends JpaRepository<RoleModuloPermiso, RoleModuloPermiso.RoleModuloPermisoId> {

    List<RoleModuloPermiso> findByRol(String rol);

    List<RoleModuloPermiso> findByRolIn(Set<String> roles);

    boolean existsByRolAndModulo(String rol, String modulo);

    @Modifying
    @Query("DELETE FROM RoleModuloPermiso r WHERE r.rol = :rol")
    void deleteByRol(String rol);

    long count();
}
