/**
 * S15: Implementación del servicio de guías GPC.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.GuiaGpcRegistroVisualizacionDto;
import com.sesa.salud.dto.GuiaGpcSugerenciaDto;
import com.sesa.salud.entity.GpcSugerenciaMostrada;
import com.sesa.salud.entity.GuiaGpc;
import com.sesa.salud.entity.Personal;
import com.sesa.salud.repository.GpcSugerenciaMostradaRepository;
import com.sesa.salud.repository.GuiaGpcRepository;
import com.sesa.salud.repository.PersonalRepository;
import com.sesa.salud.security.JwtPrincipal;
import com.sesa.salud.service.GuiaGpcService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GuiaGpcServiceImpl implements GuiaGpcService {

    private final GuiaGpcRepository guiaGpcRepository;
    private final GpcSugerenciaMostradaRepository sugerenciaMostradaRepository;
    private final PersonalRepository personalRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GuiaGpcSugerenciaDto> sugerirPorCie10(String codigoCie10) {
        if (codigoCie10 == null || codigoCie10.isBlank()) {
            return List.of();
        }
        String codigo = codigoCie10.trim().toUpperCase();
        List<GuiaGpc> guias = guiaGpcRepository.findByCodigoCie10OrderByTituloAsc(codigo);
        if (guias.isEmpty()) {
            guias = guiaGpcRepository.findByCodigoCie10StartingWithOrderByTituloAsc(codigo);
        }
        List<GuiaGpcSugerenciaDto> result = new ArrayList<>();
        for (GuiaGpc g : guias) {
            result.add(GuiaGpcSugerenciaDto.builder()
                    .id(g.getId())
                    .titulo(g.getTitulo())
                    .criteriosControl(g.getCriteriosControl())
                    .medicamentosPrimeraLinea(g.getMedicamentosPrimeraLinea())
                    .estudiosSeguimiento(g.getEstudiosSeguimiento())
                    .fuente(g.getFuente())
                    .build());
        }
        return result;
    }

    @Override
    @Transactional
    public void registrarVisualizacion(GuiaGpcRegistroVisualizacionDto dto) {
        if (dto == null || dto.getGuiaId() == null || dto.getAtencionId() == null) {
            return;
        }
        GuiaGpc guia = guiaGpcRepository.findById(dto.getGuiaId()).orElse(null);
        if (guia == null) return;
        Personal profesional = null;
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof JwtPrincipal principal) {
                profesional = personalRepository.findByUsuario_Id(principal.userId()).orElse(null);
            }
        } catch (Exception ignored) {}
        GpcSugerenciaMostrada reg = GpcSugerenciaMostrada.builder()
                .atencionId(dto.getAtencionId())
                .codigoCie10(dto.getCodigoCie10() != null ? dto.getCodigoCie10() : guia.getCodigoCie10())
                .guia(guia)
                .profesional(profesional)
                .build();
        sugerenciaMostradaRepository.save(reg);
    }
}
