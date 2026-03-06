/**
 * Configuración de facturación electrónica DIAN por tenant (schema).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "facturacion_electronica_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturacionElectronicaConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "facturacion_activa", nullable = false)
    @Builder.Default
    private Boolean facturacionActiva = Boolean.FALSE;

    @Column(length = 20)
    private String nit;

    @Column(name = "razon_social", length = 255)
    private String razonSocial;

    @Column(name = "nombre_comercial", length = 255)
    private String nombreComercial;

    @Column(length = 50)
    private String regimen;

    @Column(length = 255)
    private String direccion;

    @Column(length = 100)
    private String municipio;

    @Column(length = 100)
    private String departamento;

    @Column(length = 100)
    private String pais;

    @Column(name = "email_contacto", length = 255)
    private String emailContacto;

    @Column(length = 20, nullable = false)
    @Builder.Default
    private String ambiente = "HABILITACION"; // HABILITACION / PRODUCCION

    @Column(name = "numero_resolucion", length = 50)
    private String numeroResolucion;

    @Column(name = "fecha_resolucion")
    private java.time.LocalDate fechaResolucion;

    @Column(length = 10)
    private String prefijo;

    @Column(name = "rango_desde")
    private Long rangoDesde;

    @Column(name = "rango_hasta")
    private Long rangoHasta;

    @Column(name = "clave_tecnica", length = 128)
    private String claveTecnica;

    @Column(name = "software_id", length = 64)
    private String softwareId;

    @Column(name = "software_pin", length = 64)
    private String softwarePin;

    @Column(name = "plantilla_pdf", length = 100)
    private String plantillaPdf;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

