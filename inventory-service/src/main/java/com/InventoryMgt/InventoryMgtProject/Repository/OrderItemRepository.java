package com.InventoryMgt.InventoryMgtProject.Repository;

import com.InventoryMgt.InventoryMgtProject.Entities.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByOrderIdIn(List<Long> orderIds);

    // Fetch items with product
    @Query("""
        SELECT oi
        FROM OrderItem oi
        JOIN FETCH oi.product
        WHERE oi.order.id = :orderId
    """)
    List<OrderItem> findByOrderIdWithProduct(Long orderId);

    // Used in reports to avoid N+1
    @Query("""
        SELECT oi
        FROM OrderItem oi
        LEFT JOIN FETCH oi.product
        WHERE oi.order.id IN :orderIds
    """)
    List<OrderItem> findByOrderIdInWithProduct(List<Long> orderIds);

    @Query("""
    SELECT 
        oi.product.id,
        oi.productName,
        SUM(oi.quantity),
        p.quantity
    FROM OrderItem oi
    JOIN Product p ON p.id = oi.product.id
    WHERE oi.order.organizationId = :organizationId
    AND oi.order.orderStatus <> 'PENDING'
    GROUP BY oi.product.id, oi.productName, p.quantity
    ORDER BY SUM(oi.quantity) DESC
""")
    List<Object[]> findProductDemandStats(
            @Param("organizationId") String organizationId
    );


    //for high selling categories
    @Query("""
SELECT p.category.id, c.name, SUM(oi.quantity)
FROM OrderItem oi
JOIN oi.product p
JOIN p.category c
WHERE oi.order.organizationId = :orgId
GROUP BY p.category.id, c.name
ORDER BY SUM(oi.quantity) DESC
""")
    List<Object[]> getCategoryStats(String orgId);

}