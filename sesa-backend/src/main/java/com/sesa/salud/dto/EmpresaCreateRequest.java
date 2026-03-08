/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaCreateRequest {

    /** Paso 1: información general */
    @NotBlank(message = "Schema es obligatorio")
    private String schemaName;
    @NotBlank(message = "Razón social es obligatoria")
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

    /** Paso 2: módulos (códigos) */
    private List<String> moduloCodigos;

    /** Paso 2b: submódulos (códigos) */
    private List<String> submoduloCodigos;

    /** Paso 3: límites de usuarios */
    private Integer usuarioMovilLimit = 0;
    private Integer usuarioWebLimit = 0;

    /** Paso 4: usuario administrador */
    @NotNull(message = "Datos del administrador son obligatorios")
    @Valid
    private AdminUserRequest adminUser;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminUserRequest {
        @NotBlank(message = "Identificación es obligatoria")
        private String identificacion;
        @NotBlank(message = "Primer nombre es obligatorio")
        private String primerNombre;
        private String segundoNombre;
        @NotBlank(message = "Primer apellido es obligatorio")
        private String primerApellido;
        private String segundoApellido;
        @NotBlank(message = "Teléfono celular es obligatorio")
        private String telefonoCelular;
        @NotBlank(message = "Correo es obligatorio")
        private String correo;
        @NotBlank(message = "Contraseña es obligatoria")
        private String contraseña;
    }
}
