/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.auth.AccesoAuditoriaDto;
import com.sesa.salud.dto.auth.LoginRequest;
import com.sesa.salud.dto.auth.LoginResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuthService {

    LoginResponse login(LoginRequest request);
    String requestPasswordReset(String email);
    void resetPassword(String token, String newPassword);
    Page<AccesoAuditoriaDto> listAuditoria(Pageable pageable);
}
