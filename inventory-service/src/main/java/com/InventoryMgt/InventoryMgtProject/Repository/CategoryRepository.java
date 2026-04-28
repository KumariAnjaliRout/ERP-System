package com.InventoryMgt.InventoryMgtProject.Repository;


import com.InventoryMgt.InventoryMgtProject.Entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Prevent duplicate category names inside organization
    boolean existsByNameAndOrganizationId(String name, String organizationId);

    // Get all categories for organization
    List<Category> findByOrganizationId(String organizationId);

    // Get category by id + organization
    Optional<Category> findByIdAndOrganizationId(Long id, String organizationId);

    // Fetch categories with products (avoid N+1)
    @Query("""
        SELECT DISTINCT c
        FROM Category c
        LEFT JOIN FETCH c.products
        WHERE c.organizationId = :organizationId
    """)
    List<Category> findCategoriesWithProducts(
            @Param("organizationId") String organizationId
    );
}