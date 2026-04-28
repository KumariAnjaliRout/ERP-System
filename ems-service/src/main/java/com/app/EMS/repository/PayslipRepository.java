package com.app.EMS.repository;

import com.app.EMS.entity.Payroll;
import com.app.EMS.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayslipRepository
        extends JpaRepository<Payslip, Long> {

    List<Payslip> findByEmployeeId(String employeeId);

    Optional<Payslip> findByPayroll_Id(Long payrollId);

    List<Payslip> findByEmployeeIdIn(List<String> employeeId);
}
