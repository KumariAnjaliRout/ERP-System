package com.erp.accountantservice.repository;

import com.erp.accountantservice.entity.Expense;
import com.erp.accountantservice.entity.Revenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.query.Param;

@Repository
public interface RevenueRepository extends JpaRepository<Revenue, UUID> {
    List<Revenue> findByOutletId(String outletId);
    List<Revenue> findByOrganizationId(String organizationId);

    // Organization-wise revenue
    @Query("SELECT r.organizationId as orgId, SUM(r.amount) as total " +
            "FROM Revenue r GROUP BY r.organizationId")
    List<Object[]> getRevenueByOrganization();

    // Accountant-wise (mock grouping for now)
    @Query("SELECT r.outletId as outletId, SUM(r.amount) as total " +
            "FROM Revenue r GROUP BY r.outletId")
    List<Object[]> getRevenueByOutlet();


    @Query(value = """
    SELECT 
        r.outlet_id as outletId,
        o.name as outletName,
        TO_CHAR(r.revenue_date, 'YYYY-MM') as month,
        COALESCE(SUM(r.amount), 0) as revenue
    FROM revenue r
    JOIN outlets o ON r.outlet_id = o.outlet_code
    WHERE r.organization_id = :orgId
    GROUP BY r.outlet_id, o.name, TO_CHAR(r.revenue_date, 'YYYY-MM')
    ORDER BY month DESC, revenue DESC
    """, nativeQuery = true)
    List<Object[]> findMonthlyRevenueByOutlet(@Param("orgId") String orgId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.organizationId = :orgId AND LOWER(e.expenseCategory) LIKE '%salary%'")
    BigDecimal getTotalSalaryByOrganization(@Param("orgId") String orgId);

    @Query("SELECT COUNT(DISTINCT e.accountantId) FROM Expense e WHERE e.organizationId = :orgId AND LOWER(e.expenseCategory) LIKE '%salary%'")
    Long getEmployeeCountByOrganization(@Param("orgId") String orgId);

    //============== Monthly breakdown ------------------
    @Query(value = """
    SELECT TO_CHAR(revenue_date, 'YYYY-MM') as month,
           SUM(amount) as total
    FROM revenue
    WHERE organization_id = :orgId
    GROUP BY TO_CHAR(revenue_date, 'YYYY-MM')
    ORDER BY month
""", nativeQuery = true)
    List<Object[]> getMonthlyBreakdown(@Param("orgId") String orgId);

    // ------------ weekly breakdown --------------------
    @Query(value = """
    SELECT TO_CHAR(revenue_date, 'IYYY-IW') as week,
           SUM(amount) as total
    FROM revenue
    WHERE organization_id = :orgId
    GROUP BY TO_CHAR(revenue_date, 'IYYY-IW')
    ORDER BY week
""", nativeQuery = true)
    List<Object[]> getWeeklyBreakdown(@Param("orgId") String orgId);


    // ------------------- yearly breakdown ----------------
    @Query(value = """
    SELECT EXTRACT(YEAR FROM revenue_date) as year,
           SUM(amount) as total
    FROM revenue
    WHERE organization_id = :orgId
    GROUP BY EXTRACT(YEAR FROM revenue_date)
    ORDER BY year
""", nativeQuery = true)
    List<Object[]> getYearlyBreakdown(@Param("orgId") String orgId);

    @Query("""
    SELECT r.organizationId, SUM(r.amount)
    FROM Revenue r
    GROUP BY r.organizationId
""")
    List<Object[]> getOrganizationWiseRevenue();

    @Query("SELECT r FROM Revenue r WHERE r.revenueDate >= :start AND r.revenueDate <= :end")
    List<Revenue> findByRevenueDateBetween(@org.springframework.data.repository.query.Param("start") LocalDate start, @org.springframework.data.repository.query.Param("end") LocalDate end);

    @Query("SELECT e FROM Expense e WHERE e.expenseDate BETWEEN :start AND :end")
    List<Expense> findByExpenseDateBetween(@org.springframework.data.repository.query.Param("start") LocalDate start, @org.springframework.data.repository.query.Param("end") LocalDate end);


}





