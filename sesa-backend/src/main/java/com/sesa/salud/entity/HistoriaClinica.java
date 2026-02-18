/**
 * Entidad Historia Clínica - Documento central del paciente
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "historias_clinicas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriaClinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false, unique = true)
    private Paciente paciente;

    @Column(name = "fecha_apertura", nullable = false)
    private Instant fechaApertura;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "ACTIVA";

    @Column(name = "grupo_sanguineo", length = 10)
    private String grupoSanguineo;

    @Column(columnDefinition = "TEXT")
    private String alergiasGenerales;

    @Column(name = "antecedentes_personales", columnDefinition = "TEXT")
    private String antecedentesPersonales;

    @Column(name = "antecedentes_quirurgicos", columnDefinition = "TEXT")
    private String antecedentesQuirurgicos;

    @Column(name = "antecedentes_farmacologicos", columnDefinition = "TEXT")
    private String antecedentesFarmacologicos;

    @Column(name = "antecedentes_traumaticos", columnDefinition = "TEXT")
    private String antecedentesTraumaticos;

    @Column(name = "antecedentes_ginecoobstetricos", columnDefinition = "TEXT")
    private String antecedentesGinecoobstetricos;

    @Column(name = "antecedentes_familiares", columnDefinition = "TEXT")
    private String antecedentesFamiliares;

    @Column(name = "habitos_tabaco")
    private Boolean habitosTabaco;

    @Column(name = "habitos_alcohol")
    private Boolean habitosAlcohol;

    @Column(name = "habitos_sustancias")
    private Boolean habitosSustancias;

    @Column(name = "habitos_detalles", columnDefinition = "TEXT")
    private String habitosDetalles;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "historiaClinica", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Atencion> atenciones = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (fechaApertura == null) {
            fechaApertura = Instant.now();
        }
    }
}
