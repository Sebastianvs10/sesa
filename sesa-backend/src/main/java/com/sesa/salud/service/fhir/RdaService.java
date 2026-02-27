/**
 * Servicio principal del ciclo de vida del RDA
 * Generación, persistencia y envío a la plataforma IHCE
 * Resolución 1888 de 2025 — Colombia
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.fhir;

import com.sesa.salud.dto.rda.RdaStatusDto;
import com.sesa.salud.entity.Atencion;
import com.sesa.salud.entity.RdaEnvio;
import com.sesa.salud.repository.AtencionRepository;
import com.sesa.salud.repository.RdaEnvioRepository;
import com.sesa.salud.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RdaService {

    private final AtencionRepository   atencionRepository;
    private final RdaEnvioRepository   rdaEnvioRepository;
    private final RdaGeneratorService  generatorService;
    private final RdaMinisterioClient  ministerioClient;

    // ═══════════════════════════════════════════════════════════════════════
    //  GENERAR RDA
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Genera (o regenera) el Bundle FHIR para una atención.
     * Si ya existe un RDA PENDIENTE lo reemplaza; si está ENVIADO/CONFIRMADO, crea uno nuevo.
     */
    @Transactional
    public RdaStatusDto generarRda(Long atencionId, RdaEnvio.TipoRda tipoRda) {
        Atencion atencion = atencionRepository.findById(atencionId)
                .orElseThrow(() -> new RuntimeException("Atención no encontrada: " + atencionId));

        String bundleJson = switch (tipoRda) {
            case CONSULTA_EXTERNA -> generatorService.generarRdaConsultaExterna(atencion);
            case PACIENTE         -> generatorService.generarRdaPaciente(atencion);
            default               -> generatorService.generarRdaConsultaExterna(atencion);
        };

        RdaEnvio rdaEnvio = RdaEnvio.builder()
                .atencionId(atencionId)
                .tipoRda(tipoRda)
                .estadoEnvio(RdaEnvio.EstadoRda.PENDIENTE)
                .bundleJson(bundleJson)
                .fechaGeneracion(Instant.now())
                .tenantSchema(TenantContextHolder.getTenantSchema())
                .build();

        rdaEnvio = rdaEnvioRepository.save(rdaEnvio);
        log.info("RDA generado — id:{} atencion:{} tipo:{}", rdaEnvio.getId(), atencionId, tipoRda);
        return toDto(rdaEnvio);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ENVIAR AL MINISTERIO
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Envía el RDA más reciente (PENDIENTE) de una atención al Ministerio.
     */
    @Transactional
    public RdaStatusDto enviarAlMinisterio(Long atencionId, RdaEnvio.TipoRda tipoRda) {
        RdaEnvio rdaEnvio = rdaEnvioRepository
                .findFirstByAtencionIdAndTipoRdaOrderByFechaGeneracionDesc(atencionId, tipoRda)
                .orElseThrow(() -> new RuntimeException(
                        "No hay RDA generado para la atención " + atencionId +
                        ". Genérelo primero."));

        if (rdaEnvio.getEstadoEnvio() == RdaEnvio.EstadoRda.CONFIRMADO) {
            throw new RuntimeException("El RDA ya fue confirmado por el Ministerio.");
        }

        try {
            String idMinisterio = ministerioClient.enviarBundle(rdaEnvio.getBundleJson());
            rdaEnvio.setEstadoEnvio(RdaEnvio.EstadoRda.ENVIADO);
            rdaEnvio.setIdMinisterio(idMinisterio);
            rdaEnvio.setFechaEnvio(Instant.now());
            rdaEnvio.setErrorMensaje(null);
            log.info("RDA enviado al Ministerio — rdaId:{} idMinsalud:{}", rdaEnvio.getId(), idMinisterio);
        } catch (Exception e) {
            rdaEnvio.setEstadoEnvio(RdaEnvio.EstadoRda.ERROR);
            rdaEnvio.setErrorMensaje(e.getMessage());
            rdaEnvio.setReintentos(rdaEnvio.getReintentos() + 1);
            log.error("Error enviando RDA al Ministerio: {}", e.getMessage());
        }

        rdaEnvio = rdaEnvioRepository.save(rdaEnvio);
        return toDto(rdaEnvio);
    }

    /**
     * Genera Y envía el RDA en un solo paso (flujo recomendado).
     */
    @Transactional
    public RdaStatusDto generarYEnviar(Long atencionId, RdaEnvio.TipoRda tipoRda) {
        generarRda(atencionId, tipoRda);
        return enviarAlMinisterio(atencionId, tipoRda);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CONSULTAS
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<RdaStatusDto> listarPorAtencion(Long atencionId) {
        return rdaEnvioRepository
                .findByAtencionIdOrderByFechaGeneracionDesc(atencionId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public RdaStatusDto obtenerUltimo(Long atencionId, RdaEnvio.TipoRda tipoRda) {
        return rdaEnvioRepository
                .findFirstByAtencionIdAndTipoRdaOrderByFechaGeneracionDesc(atencionId, tipoRda)
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public String obtenerBundleJson(Long rdaId, boolean pretty) {
        RdaEnvio envio = rdaEnvioRepository.findById(rdaId)
                .orElseThrow(() -> new RuntimeException("RDA no encontrado: " + rdaId));
        return envio.getBundleJson();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  MAPPER
    // ═══════════════════════════════════════════════════════════════════════

    private RdaStatusDto toDto(RdaEnvio e) {
        return RdaStatusDto.builder()
                .rdaId(e.getId())
                .atencionId(e.getAtencionId())
                .tipoRda(e.getTipoRda())
                .estadoEnvio(e.getEstadoEnvio())
                .idMinisterio(e.getIdMinisterio())
                .fechaGeneracion(e.getFechaGeneracion())
                .fechaEnvio(e.getFechaEnvio())
                .fechaConfirmacion(e.getFechaConfirmacion())
                .errorMensaje(e.getErrorMensaje())
                .reintentos(e.getReintentos())
                .build();
    }
}
