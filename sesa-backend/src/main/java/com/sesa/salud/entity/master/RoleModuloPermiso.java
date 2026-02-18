/**
 * Entidad: permiso rol-módulo (sistema RBAC editable).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity.master;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "role_modulo_permiso", schema = "public", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"rol", "modulo"})
})
@IdClass(RoleModuloPermiso.RoleModuloPermisoId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleModuloPermiso {

    @Id
    @Column(name = "rol", nullable = false, length = 50)
    private String rol;

    @Id
    @Column(name = "modulo", nullable = false, length = 50)
    private String modulo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleModuloPermisoId implements Serializable {
        private String rol;
        private String modulo;
    }
}
