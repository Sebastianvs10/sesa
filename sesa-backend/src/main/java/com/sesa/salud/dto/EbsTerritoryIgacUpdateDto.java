/**
 * DTO para actualizar códigos IGAC de un territorio EBS.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EbsTerritoryIgacUpdateDto {
    private String igacDepartamentoCodigo;
    private String igacMunicipioCodigo;
    private String igacVeredaCodigo;
}
