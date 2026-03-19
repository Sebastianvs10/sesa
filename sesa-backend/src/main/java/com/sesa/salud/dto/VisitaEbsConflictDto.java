/**
 * S13: Detalle de un conflicto de sincronización EBS.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitaEbsConflictDto {
    private String offlineUuid;
    private Long serverId;
    private String message;
}
