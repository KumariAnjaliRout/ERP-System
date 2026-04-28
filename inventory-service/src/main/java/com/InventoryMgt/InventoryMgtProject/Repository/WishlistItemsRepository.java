package com.InventoryMgt.InventoryMgtProject.Repository;

import com.InventoryMgt.InventoryMgtProject.Entities.WishlistItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemsRepository extends JpaRepository<WishlistItems, Long> {

    Optional<WishlistItems> findByWishlistIdAndProductId(Long wishlistId, Long productId);

    boolean existsByWishlistIdAndProductId(Long wishlistId, Long productId);

    void deleteByWishlistId(Long wishlistId);

    long countByWishlistId(Long wishlistId);

    // Fetch wishlist items with product
    @Query("""
      SELECT wi
      FROM WishlistItems wi
      JOIN FETCH wi.product
      WHERE wi.wishlist.id = :wishlistId
    """)
    List<WishlistItems> findByWishlistIdWithProduct(Long wishlistId);
}