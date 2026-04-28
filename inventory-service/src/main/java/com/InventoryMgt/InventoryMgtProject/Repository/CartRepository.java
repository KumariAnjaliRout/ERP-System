package com.InventoryMgt.InventoryMgtProject.Repository;


import com.InventoryMgt.InventoryMgtProject.Entities.Cart;
import com.InventoryMgt.InventoryMgtProject.Entities.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {


    @Query("""
    SELECT c
    FROM Cart c
    LEFT JOIN FETCH c.items i
    LEFT JOIN FETCH i.product
    WHERE c.outletId = :outletId
    AND c.status = 'ACTIVE'
    """)
    Optional<Cart> findActiveCartWithItems(String outletId);
}