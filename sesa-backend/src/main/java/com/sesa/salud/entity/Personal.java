/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    // ── Tipo de documento (CC, CE, PA, PEP, TI, RC — Res. 3374/2000 RIPS) ──
    @Column(name = "tipo_documento", length = 10)
    private String tipoDocumento;

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

    /** Correo profesional para documentos clínicos (distinto al email de login en Usuario). */
    @Column(length = 255)
    private String email;

    /** Rol primario (fuente de verdad: Personal.roles). Se mantiene por compatibilidad. */
    @Column(length = 50)
    private String rol;

    /**
     * Multi-rol profesional. Un mismo profesional puede tener más de un rol clínico
     * (ej. MEDICO + COORDINADOR_MEDICO). Se sincroniza con Usuario.roles al guardar.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "personal_roles", joinColumns = @JoinColumn(name = "personal_id"))
    @Column(name = "rol")
    @Builder.Default
    private Set<String> roles = new HashSet<>();

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

    // ── Campos normativos Res. 2003/2014 (habilitación), Ley 23/1981, Res. 1449/2016 ──
    @Column(name = "tarjeta_profesional", length = 30)
    private String tarjetaProfesional;

    @Column(name = "especialidad_formal", length = 150)
    private String especialidadFormal;

    @Column(name = "numero_rethus", length = 30)
    private String numeroRethus;

    // ── Datos demográficos (RIPS Res. 3374/2000, SISPRO) ──
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(length = 10)
    private String sexo;

    // ── Lugar de práctica (Res. 2003/2014 habilitación) ──
    @Column(length = 10)
    private String municipio;

    @Column(length = 10)
    private String departamento;

    // ── Vínculo laboral (Res. 2003/2014, Circular 047/2007 Min. Protección Social) ──
    @Column(name = "tipo_vinculacion", length = 30)
    private String tipoVinculacion;

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    @Column(name = "fecha_retiro")
    private LocalDate fechaRetiro;

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
