/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "personal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Personal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombres", nullable = false, length = 150)
    private String nombres;

    @Column(length = 150)
    private String apellidos;

    @Column(nullable = false, length = 100)
    private String cargo;

    @Column(length = 100)
    private String servicio;

    @Column(length = 50)
    private String turno;

    @Column(length = 50)
    private String identificacion;

    @Column(name = "primer_nombre", length = 80)
    private String primerNombre;

    @Column(name = "segundo_nombre", length = 80)
    private String segundoNombre;

    @Column(name = "primer_apellido", length = 80)
    private String primerApellido;

    @Column(name = "segundo_apellido", length = 80)
    private String segundoApellido;

    @Column(length = 30)
    private String celular;

    @Column(length = 255)
    private String email;

    @Column(length = 50)
    private String rol;

    @Column(name = "institucion_prestadora", length = 255)
    private String institucionPrestadora;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @JdbcTypeCode(Types.BINARY)
    @Column(name = "foto_data", columnDefinition = "bytea")
    private byte[] fotoData;

    @Column(name = "foto_content_type", length = 100)
    private String fotoContentType;

    @Column(name = "firma_url", length = 500)
    private String firmaUrl;

    @JdbcTypeCode(Types.BINARY)
    @Column(name = "firma_data", columnDefinition = "bytea")
    private byte[] firmaData;

    @Column(name = "firma_content_type", length = 100)
    private String firmaContentType;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "profesional", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Cita> citasAtendidas = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
