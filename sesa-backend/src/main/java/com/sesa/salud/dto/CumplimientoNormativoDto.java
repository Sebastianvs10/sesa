/**
 * Panel de cumplimiento normativo (S4): indicadores por período y profesional.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CumplimientoNormativoDto {

    private LocalDate periodoInicio;
    private LocalDate periodoFin;
    private Long profesionalId;

    /** % de atenciones con RDA enviado/confirmado sobre total atenciones en el período. */
    private Double porcentajeRdaEnviado;
    private long totalAtenciones;
    private long atencionesConRdaEnviado;

    /** % urgencias atendidas dentro del tiempo según triage (Res. 5596/2015). */
    private Double porcentajeUrgenciasEnTiempo;
    private long totalUrgenciasAtendidas;
    private long urgenciasDentroTiempo;

    /** % atenciones con CIE-10 y evolución registrada en menos de 24 h. */
    private Double porcentajeHcConCie10YEvolucion24h;
    private long atencionesConCie10YEvolucion24h;

    /** Total de órdenes con resultado crítico sin registro de lectura. */
    private long totalResultadosCriticosNoLeidos;

    /** Indicadores Res. 0256/2016 (calidad en salud). */
    private List<IndicadorCalidadDto> indicadores0256;
}
