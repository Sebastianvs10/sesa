/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.OrdenClinicaItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdenClinicaItemRepository extends JpaRepository<OrdenClinicaItem, Long> {
    List<OrdenClinicaItem> findByOrden_IdOrderByOrdenItemIndexAsc(Long ordenId);
}
