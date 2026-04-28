package com.InventoryMgt.InventoryMgtProject.Repository;

import com.InventoryMgt.InventoryMgtProject.Entities.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Prevent duplicate product names inside organization
    boolean existsByNameAndOrganizationId(String name, String organizationId);

    // Get all products for organization
    List<Product> findByOrganizationId(String organizationId);

    // Get product by id + organization
    Optional<Product> findByIdAndOrganizationId(Long id, String organizationId);

    // Lock product row during checkout (prevent stock race condition)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM Product p
        WHERE p.id = :productId
        AND p.organizationId = :organizationId
    """)
    Optional<Product> findProductForUpdate(
            @Param("productId") Long productId,
            @Param("organizationId") String organizationId
    );

    // Fetch products with category (avoid N+1)
    @Query("""
        SELECT p
        FROM Product p
        JOIN FETCH p.category
        WHERE p.organizationId = :organizationId
    """)
    List<Product> findProductsWithCategory(
            @Param("organizationId") String organizationId
    );

    // ---------- INVENTORY STATS ----------

    long countByOrganizationId(String organizationId);

    @Query("""
        SELECT COUNT(p)
        FROM Product p
        WHERE p.organizationId = :organizationId
        AND p.quantity = 0
    """)
    long countOutOfStockProducts(@Param("organizationId") String organizationId);

    @Query("""
        SELECT COUNT(p)
        FROM Product p
        WHERE p.organizationId = :organizationId
        AND p.quantity BETWEEN 1 AND 9
    """)
    long countLowStockProducts(@Param("organizationId") String organizationId);

    @Query("""
        SELECT SUM(p.quantity * p.totalPrice)
        FROM Product p
        WHERE p.organizationId = :organizationId
    """)
    BigDecimal calculateTotalInventoryValue(
            @Param("organizationId") String organizationId
    );

    @Query("""
    SELECT p.id, p.name, p.quantity
    FROM Product p
    WHERE p.organizationId = :orgId
    """)
    List<Object[]> getStockData(String orgId);
}