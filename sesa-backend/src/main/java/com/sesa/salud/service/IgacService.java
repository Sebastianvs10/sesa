/**
 * Servicio catálogo IGAC – Límites oficiales (Departamentos, Municipios, Veredas).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.IgacDepartamentoDto;
import com.sesa.salud.dto.IgacMunicipioDto;
import com.sesa.salud.dto.IgacVeredaDto;

import java.util.List;
import java.util.Optional;

public interface IgacService {

    List<IgacDepartamentoDto> listDepartamentos();

    List<IgacMunicipioDto> listMunicipiosPorDepartamento(String departamentoCodigo);

    List<IgacVeredaDto> listVeredasPorMunicipio(String municipioCodigo);

    Optional<IgacDepartamentoDto> getDepartamentoByCodigo(String codigoDane);

    Optional<IgacMunicipioDto> getMunicipioByCodigo(String codigoDane);

    Optional<IgacVeredaDto> getVeredaByCodigo(String codigo);

    /** GeoJSON de una vereda (si existe geometryJson). */
    Optional<String> getVeredaGeojson(String codigo);
}
