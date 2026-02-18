/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.EmpresaCreateRequest;

public interface TenantSetupService {

    String createTenantSchemaAndAdmin(String schemaName, EmpresaCreateRequest request);
}
