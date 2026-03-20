/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.auth.AccesoAuditoriaDto;
import com.sesa.salud.dto.auth.LoginRequest;
import com.sesa.salud.dto.auth.LoginResponse;
import com.sesa.salud.dto.auth.PasswordResetRequestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuthService {

    LoginResponse login(LoginRequest request);
    PasswordResetRequestResponse requestPasswordReset(String email);
    void resetPassword(String token, String newPassword);
    Page<AccesoAuditoriaDto> listAuditoria(Pageable pageable);
}
