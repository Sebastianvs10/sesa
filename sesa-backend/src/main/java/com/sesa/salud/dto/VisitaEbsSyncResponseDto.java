/**
 * S13: Respuesta del endpoint de sincronización EBS.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitaEbsSyncResponseDto {
    @Builder.Default
    private List<Long> savedIds = new ArrayList<>();
    @Builder.Default
    private List<VisitaEbsConflictDto> conflicts = new ArrayList<>();
}
