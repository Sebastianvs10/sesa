/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity.master;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "empresa_modulos", schema = "public",
       uniqueConstraints = @UniqueConstraint(columnNames = {"empresa_id", "modulo_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(EmpresaModulo.EmpresaModuloId.class)
public class EmpresaModulo {

    @Id
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Id
    @Column(name = "modulo_id", nullable = false)
    private Long moduloId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false, insertable = false, updatable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modulo_id", nullable = false, insertable = false, updatable = false)
    private Modulo modulo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmpresaModuloId implements Serializable {
        private Long empresaId;
        private Long moduloId;
    }
}
