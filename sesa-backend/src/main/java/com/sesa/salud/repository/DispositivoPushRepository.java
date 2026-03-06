/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.DispositivoPush;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DispositivoPushRepository extends JpaRepository<DispositivoPush, Long> {

    List<DispositivoPush> findByUsuarioId(Long usuarioId);

    Optional<DispositivoPush> findByUsuarioIdAndToken(Long usuarioId, String token);

    void deleteByUsuarioIdAndToken(Long usuarioId, String token);
}
