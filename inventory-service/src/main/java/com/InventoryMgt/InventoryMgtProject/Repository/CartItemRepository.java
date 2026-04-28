package com.InventoryMgt.InventoryMgtProject.Repository;


import com.InventoryMgt.InventoryMgtProject.Entities.CartItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItems, Long> {

    List<CartItems> findByCartId(Long cartId);

    Optional<CartItems> findByIdAndCartOutletId(Long id, String outletId);

    Optional<CartItems> findByCartIdAndProductId(Long cartId, Long productId);

    void deleteByCartId(Long cartId);

    // Fetch cart items with product
    @Query("""
        SELECT ci
        FROM CartItems ci
        JOIN FETCH ci.product
        WHERE ci.cart.id = :cartId
    """)
    List<CartItems> findByCartIdWithProduct(Long cartId);
}