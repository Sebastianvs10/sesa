/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pacientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_documento", length = 10)
    private String tipoDocumento;

    @Column(nullable = false, unique = true, length = 50)
    private String documento;

    @Column(nullable = false, length = 150)
    private String nombres;

    @Column(length = 150)
    private String apellidos;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(length = 20)
    private String sexo;

    @Column(name = "grupo_sanguineo", length = 10)
    private String grupoSanguineo;

    @Column(length = 30)
    private String telefono;

    @Column(length = 255)
    private String email;

    @Column(length = 255)
    private String direccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eps_id")
    private Eps eps;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    // Campos normativos Res. 3374/2000 (RIPS - Archivo CT)
    @Column(name = "municipio_residencia", length = 10)
    private String municipioResidencia;

    @Column(name = "departamento_residencia", length = 10)
    private String departamentoResidencia;

    @Column(name = "zona_residencia", length = 10)
    private String zonaResidencia;

    @Column(name = "regimen_afiliacion", length = 20)
    private String regimenAfiliacion;

    @Column(name = "tipo_usuario", length = 30)
    private String tipoUsuario;

    // Contacto de emergencia / acudiente
    @Column(name = "contacto_emergencia_nombre", length = 150)
    private String contactoEmergenciaNombre;

    @Column(name = "contacto_emergencia_telefono", length = 30)
    private String contactoEmergenciaTelefono;

    // Datos sociodemográficos para SISPRO
    @Column(name = "estado_civil", length = 20)
    private String estadoCivil;

    @Column(length = 50)
    private String escolaridad;

    @Column(length = 100)
    private String ocupacion;

    @Column(name = "pertenencia_etnica", length = 50)
    private String pertenenciaEtnica;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "paciente", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HistoriaClinica> historiasClinicas = new ArrayList<>();

    @OneToMany(mappedBy = "paciente", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Consulta> consultas = new ArrayList<>();

    @OneToMany(mappedBy = "paciente", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Cita> citas = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
