/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity.master;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "empresa_submodulos", schema = "public",
       uniqueConstraints = @UniqueConstraint(columnNames = {"empresa_id", "submodulo_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(EmpresaSubmodulo.EmpresaSubmoduloId.class)
public class EmpresaSubmodulo {

    @Id
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Id
    @Column(name = "submodulo_id", nullable = false)
    private Long submoduloId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false, insertable = false, updatable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submodulo_id", nullable = false, insertable = false, updatable = false)
    private Submodulo submodulo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmpresaSubmoduloId implements Serializable {
        private Long empresaId;
        private Long submoduloId;
    }
}
