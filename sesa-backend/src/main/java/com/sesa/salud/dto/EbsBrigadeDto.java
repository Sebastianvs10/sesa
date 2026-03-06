/**
 * DTO brigada EBS (respuesta y creación).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EbsBrigadeDto {

    private Long id;
    private String name;
    @NotNull
    private Long territoryId;
    private String territoryName;
    private LocalDate dateStart;
    private LocalDate dateEnd;
    private String status;
    private String notes;
    private List<Long> teamMemberIds;
    private List<String> teamMemberNames;
}
