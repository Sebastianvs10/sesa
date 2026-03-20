/**
 * S9: Gestión de glosas (rechazos de factura) y reporte recuperación de cartera.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.GlosaDto;
import com.sesa.salud.dto.GlosaRequestDto;
import com.sesa.salud.dto.RecuperacionCarteraDto;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

public interface GlosaService {

    GlosaDto create(GlosaRequestDto dto, Long creadoPorId);

    GlosaDto update(Long id, GlosaRequestDto dto);

    GlosaDto findById(Long id);

    List<GlosaDto> findByFacturaId(Long facturaId);

    List<GlosaDto> list(String estado, Instant desde, Instant hasta, Long facturaId);

    GlosaDto cambiarEstado(Long id, String estado);

    GlosaDto addAdjunto(Long glosaId, String nombreArchivo, String tipo, String urlOBlob);

    /** Sube archivo y registra como adjunto (guarda referencia; el almacenamiento real puede ser en disco/S3). */
    GlosaDto uploadAdjunto(Long glosaId, MultipartFile file);

    RecuperacionCarteraDto recuperacionCartera(Instant desde, Instant hasta, Long contratoId);
}
