package com.InventoryMgt.InventoryMgtProject.Repository;


import com.InventoryMgt.InventoryMgtProject.Entities.Order;
import com.InventoryMgt.InventoryMgtProject.Entities.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // ---------- ORDER VALIDATION ----------
    boolean existsByCartId(Long cartId);

    List<Order> findByOutletIdOrderByCreatedAtDesc(String outletId);

    // ---------- ORDER FETCH ----------
    List<Order> findByOrganizationIdAndOrderStatus(
            String organizationId,
            OrderStatus orderStatus
    );
    List<Order> findByOutletIdAndOrderStatus(
            String outletId,
            OrderStatus orderStatus
    );

    List<Order> findByOrganizationIdAndOutletIdAndOrderStatus(
            String organizationId,
            String outletId,
            OrderStatus orderStatus
    );




    // ---------- ORDER DELETE ----------
    @Modifying
    @Transactional
    @Query("""
        DELETE FROM Order o
        WHERE o.orderStatus = :status
        AND o.organizationId = :organizationId
    """)
    int deletePendingOrders(
            @Param("status") OrderStatus status,
            @Param("organizationId") String organizationId
    );


    // ---------- ORDER COUNT ----------
    Long countByOrderStatusAndOrganizationId(
            OrderStatus status,
            String organizationId
    );


    // ---------- REPORTING (OUTLET LEVEL) ----------
    List<Order> findByOrganizationIdAndOutletIdInAndOrderStatusAndCreatedAtBetween(
            String organizationId,
            List<String> outletIds,
            OrderStatus orderStatus,
            Instant start,
            Instant end
    );

    // ---------- RECENT ORDERS ----------
    List<Order> findTop10ByOrderByCreatedAtDesc();


    // ---------- ORG REVENUE STATS ----------
    @Query("""
        SELECT o.organizationId, COUNT(o)
        FROM Order o
        GROUP BY o.organizationId
        ORDER BY COUNT(o) DESC
    """)
    List<Object[]> findOrdersPerOrganization();


    // ---------- OUTLET ORDER COUNT ----------
    @Query("""
        SELECT o.outletId, COUNT(o)
        FROM Order o
        WHERE o.organizationId = :organizationId
        GROUP BY o.outletId
    """)
    List<Object[]> findOrderCountsByOutlet(
            @Param("organizationId") String organizationId
    );

    @Query("""
   SELECT o.organizationId, COUNT(o), SUM(o.totalAmount)
   FROM Order o
   WHERE o.orderStatus = 'DELIVERED'
   GROUP BY o.organizationId
   ORDER BY SUM(o.totalAmount) DESC
""")
    List<Object[]> findOrganizationRevenueStats();

    @Query("""
      SELECT o
      FROM Order o
      WHERE o.createdAt BETWEEN :start AND :end
      ORDER BY o.createdAt DESC
      """)
    Page<Order> findRecentOrders(
            Instant start,
            Instant end,
            Pageable pageable
    );

    @Query("""
    SELECT o
    FROM Order o
    WHERE o.createdAt BETWEEN :start AND :end
      AND o.organizationId = :orgId
    ORDER BY o.createdAt DESC
""")
    Page<Order> findRecentOrdersByOrg(
            Instant start,
            Instant end,
            String orgId,
            Pageable pageable
    );

    List<Order> findByOrganizationIdOrderByCreatedAtDesc(String organizationId);
    List<Order> findByOrganizationIdAndOrderStatusNot(
            String organizationId,
            OrderStatus status
    );

    List<Order> findByOrganizationIdAndOrderStatusAndInvoiceGeneratedTrueOrderByCreatedAtDesc(
            String organizationId,
            OrderStatus status
    );

    @Query("""
SELECT EXTRACT(YEAR FROM o.createdAt),
       EXTRACT(MONTH FROM o.createdAt),
       COUNT(o)
FROM Order o
WHERE o.organizationId = :orgId
GROUP BY EXTRACT(YEAR FROM o.createdAt), EXTRACT(MONTH FROM o.createdAt)
ORDER BY 1,2
""")
    List<Object[]> getOrdersByMonth(String orgId);


}