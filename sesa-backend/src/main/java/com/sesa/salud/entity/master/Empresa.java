/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity.master;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "empresas", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schema_name", nullable = false, unique = true, length = 63)
    private String schemaName;

    @Column(name = "razon_social", nullable = false, length = 255)
    private String razonSocial;

    @Column(length = 30)
    private String telefono;

    @Column(name = "segundo_telefono", length = 30)
    private String segundoTelefono;

    @Column(length = 50)
    private String identificacion;

    @Column(name = "direccion_empresa", length = 255)
    private String direccionEmpresa;

    @Column(name = "tipo_documento", length = 20)
    private String tipoDocumento;

    @Column(length = 50)
    private String regimen;

    @Column(name = "numero_divipola", length = 20)
    private String numeroDivipola;

    @Column(length = 100)
    private String pais;

    @Column(length = 100)
    private String departamento;

    @Column(length = 100)
    private String municipio;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "imagen_data", columnDefinition = "bytea")
    private byte[] imagenData;

    @Column(name = "imagen_content_type", length = 100)
    private String imagenContentType;

    @Column(name = "admin_email", length = 255)
    private String adminEmail;

    @Column(name = "admin_identificacion", length = 50)
    private String adminIdentificacion;

    @Column(name = "admin_primer_nombre", length = 80)
    private String adminPrimerNombre;

    @Column(name = "admin_segundo_nombre", length = 80)
    private String adminSegundoNombre;

    @Column(name = "admin_primer_apellido", length = 80)
    private String adminPrimerApellido;

    @Column(name = "admin_segundo_apellido", length = 80)
    private String adminSegundoApellido;

    @Column(name = "admin_celular", length = 30)
    private String adminCelular;

    @Column(name = "admin_proveedor_servicio", length = 50)
    private String adminProveedorServicio;

    @Column(name = "usuario_movil_limit")
    @Builder.Default
    private Integer usuarioMovilLimit = 0;

    @Column(name = "usuario_web_limit")
    @Builder.Default
    private Integer usuarioWebLimit = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
