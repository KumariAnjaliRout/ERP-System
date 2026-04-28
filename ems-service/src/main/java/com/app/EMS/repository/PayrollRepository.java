package com.app.EMS.repository;

import com.app.EMS.entity.Payroll;
import com.app.EMS.entity.PayrollStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayrollRepository
        extends JpaRepository<Payroll,Long> {
    List<Payroll> findByEmployee_UserId(UUID userId);

    List<Payroll> findByMonthAndYear(int month,int year);
    List<Payroll> findByEmployee_OrganisationAndMonthAndYear(String organisation,int month,int year);
    boolean existsByEmployee_EmployeeIdAndMonthAndYear(
            String employeeId,
            int month,
            int year
    );
    List<Payroll> findByEmployee_EmployeeId(String employeeId);
    Optional<Payroll> findByEmployee_EmployeeIdAndMonthAndYear(
            String employeeId,
            int month,
            int year
    );
    Optional<Payroll> findByEmployee_UserIdAndMonthAndYear(
            UUID userId,
            int month,
            int year
    );

    List<Payroll> findByGeneratedAtIsNotNull();
    List<Payroll> findByEmployee_OrganisationAndGeneratedAtIsNotNull(String  organisation);
    List<Payroll> findByEmployee_EmployeeIdIn(List<String> employeeIds);
    List<Payroll> findByStatus(PayrollStatus status);
    List<Payroll> findByEmployee_OrganisationAndStatus(String orgId,PayrollStatus status);
    List<Payroll> findByEmployee_IdInAndStatus(
            List<Long> employeeIds,
            PayrollStatus status
    );

    List<Payroll> findByEmployee_IdAndStatus(
            Long employeeId,
            PayrollStatus status
    );
}
