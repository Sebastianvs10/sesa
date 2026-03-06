/**
 * DTO alerta EBS.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EbsAlertDto {

    private Long id;
    @NotBlank
    private String type;
    private String veredaCodigo;
    private String municipioCodigo;
    private String departamentoCodigo;
    @NotBlank
    private String title;
    private String description;
    @NotNull
    private LocalDate alertDate;
    private String status;
    private String externalId;
}
