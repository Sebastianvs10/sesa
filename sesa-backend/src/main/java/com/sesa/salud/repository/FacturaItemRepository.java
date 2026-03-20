/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.FacturaItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacturaItemRepository extends JpaRepository<FacturaItem, Long> {

    List<FacturaItem> findByFactura_IdOrderByItemIndexAsc(Long facturaId);
}
