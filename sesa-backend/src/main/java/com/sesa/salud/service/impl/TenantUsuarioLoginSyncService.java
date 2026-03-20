/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.entity.master.TenantUsuarioLogin;
import com.sesa.salud.repository.master.TenantUsuarioLoginRepository;
import com.sesa.salud.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * Mantiene alineado el mapa global de login (public.tenant_usuario_login) cuando cambia
 * el correo de un {@link com.sesa.salud.entity.Usuario} en un tenant.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantUsuarioLoginSyncService {

    private final TenantUsuarioLoginRepository tenantUsuarioLoginRepository;

    /**
     * Actualiza la fila de login en {@code public}: elimina la entrada del correo anterior
     * (si existía y corresponde al tenant) y registra el nuevo correo.
     */
    public void renameLoginEmail(String oldEmail, String newEmail, String tenantSchema) {
        if (tenantSchema == null || tenantSchema.isBlank()) {
            throw new IllegalStateException("Schema de tenant no definido");
        }
        if (newEmail == null || newEmail.isBlank()) {
            return;
        }
        if (Objects.equals(oldEmail, newEmail)) {
            return;
        }

        String previous = TenantContextHolder.getTenantSchema();
        TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
        try {
            Optional<TenantUsuarioLogin> taken = tenantUsuarioLoginRepository.findByEmail(newEmail);
            if (taken.isPresent()) {
                if (!tenantSchema.equals(taken.get().getSchemaName())) {
                    throw new IllegalArgumentException("El correo ya está registrado para otra empresa.");
                }
                throw new IllegalArgumentException("Ya existe un acceso con ese correo.");
            }

            if (oldEmail != null && !oldEmail.isBlank()) {
                tenantUsuarioLoginRepository.findByEmail(oldEmail).ifPresent(oldRow -> {
                    if (tenantSchema.equals(oldRow.getSchemaName())) {
                        tenantUsuarioLoginRepository.delete(oldRow);
                    } else {
                        log.warn(
                                "tenant_usuario_login: correo {} asociado a schema {}, se esperaba {}",
                                oldEmail,
                                oldRow.getSchemaName(),
                                tenantSchema);
                    }
                });
            }

            tenantUsuarioLoginRepository.save(TenantUsuarioLogin.builder()
                    .email(newEmail)
                    .schemaName(tenantSchema)
                    .build());
        } finally {
            TenantContextHolder.setTenantSchema(previous);
        }
    }
}
