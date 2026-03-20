/**
 * Entidad EBS: alerta epidemiológica o geográfica (carga manual + integración futura).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "ebs_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EbsAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "vereda_codigo", length = 20)
    private String veredaCodigo;

    @Column(name = "municipio_codigo", length = 5)
    private String municipioCodigo;

    @Column(name = "departamento_codigo", length = 2)
    private String departamentoCodigo;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "alert_date", nullable = false)
    private LocalDate alertDate;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "ACTIVA";

    @Column(name = "external_id", length = 64)
    private String externalId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
