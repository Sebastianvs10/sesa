/**
 * Repositorio EBS: equipo por brigada.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.EbsBrigadeTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EbsBrigadeTeamRepository extends JpaRepository<EbsBrigadeTeam, EbsBrigadeTeam.BrigadeTeamId> {

    List<EbsBrigadeTeam> findByBrigade_Id(Long brigadeId);

    void deleteByBrigade_Id(Long brigadeId);
}
