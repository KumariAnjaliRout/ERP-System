package com.erp.accountantservice.repository;

import com.erp.accountantservice.entity.Expense;
import com.erp.accountantservice.entity.Revenue;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findByOutletId(String outletId);
    List<Expense> findByOrganizationId(String organizationId);
    // Add this method to ExpenseRepository.java
    @Query("SELECT COALESCE(SUM(e.amount), 0.0) FROM Expense e WHERE e.organizationId = :orgId")
    double findTotalAmountByOrganizationId(@Param("orgId") String orgId);


}