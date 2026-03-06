/**
 * Entidad Factura - cobro asociado al flujo clínico
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "facturas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_factura", length = 50)
    private String numeroFactura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id")
    private OrdenClinica orden;

    @Column(name = "valor_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorTotal;

    @Column(length = 30)
    @Builder.Default
    private String estado = "PENDIENTE";

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_factura", nullable = false)
    private Instant fechaFactura;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Campos normativos Decreto 4747/2007 + Res. 3047/2008 y RIPS Res. 3374/2000
    @Column(name = "codigo_cups", length = 20)
    private String codigoCups;

    @Column(name = "descripcion_cups", length = 500)
    private String descripcionCups;

    @Column(name = "tipo_servicio", length = 40)
    private String tipoServicio;

    @Column(name = "responsable_pago", length = 30)
    private String responsablePago;

    @Column(name = "cuota_moderadora", precision = 14, scale = 2)
    private BigDecimal cuotaModeradora;

    @Column(name = "numero_autorizacion_eps", length = 60)
    private String numeroAutorizacionEps;

    @Column(name = "consecutive_counter")
    private Long consecutiveCounter;

    // Campos facturación electrónica DIAN (Res. 000042 / UBL 2.1)
    @Column(name = "dian_cufe", length = 128)
    private String dianCufe;

    @Column(name = "dian_qr_url", length = 512)
    private String dianQrUrl;

    @Column(name = "dian_estado", length = 30)
    private String dianEstado;

    @Column(name = "dian_mensaje", columnDefinition = "TEXT")
    private String dianMensaje;

    @Column(name = "dian_xml_path", length = 500)
    private String dianXmlPath;

    @Column(name = "dian_pdf_path", length = 500)
    private String dianPdfPath;

    @Column(name = "dian_fecha_envio")
    private Instant dianFechaEnvio;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (fechaFactura == null) {
            fechaFactura = Instant.now();
        }
    }
}
