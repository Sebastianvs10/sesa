/**
 * Repositorio EBS: grupos familiares.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.EbsFamilyGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EbsFamilyGroupRepository extends JpaRepository<EbsFamilyGroup, Long> {
}
