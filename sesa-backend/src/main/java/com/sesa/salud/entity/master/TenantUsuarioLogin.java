/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity.master;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tenant_usuario_login", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantUsuarioLogin {

    @Id
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "schema_name", nullable = false, length = 63)
    private String schemaName;
}
