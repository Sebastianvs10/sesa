/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaDto {

    private Long id;
    private String schemaName;
    private String razonSocial;
    private String telefono;
    private String segundoTelefono;
    private String identificacion;
    private String direccionEmpresa;
    private String tipoDocumento;
    private String regimen;
    private String numeroDivipola;
    private String pais;
    private String departamento;
    private String municipio;
    private String imagenUrl;
    private String adminCorreo;
    private String adminIdentificacion;
    private String adminPrimerNombre;
    private String adminSegundoNombre;
    private String adminPrimerApellido;
    private String adminSegundoApellido;
    private String adminCelular;
    private Integer usuarioMovilLimit;
    private Integer usuarioWebLimit;
    private Boolean activo;
    private Instant createdAt;
    private List<String> moduloCodigos;
    private List<String> submoduloCodigos;
}
