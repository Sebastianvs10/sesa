/**
 * Repositorio de tokens de recuperación (tabla en public).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository.master;

import com.sesa.salud.entity.master.PasswordResetTokenPublic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenPublicRepository extends JpaRepository<PasswordResetTokenPublic, Long> {

    Optional<PasswordResetTokenPublic> findByToken(String token);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PasswordResetTokenPublic t WHERE t.email = :email AND t.usado = false")
    int deletePendingByEmail(@Param("email") String email);
}
