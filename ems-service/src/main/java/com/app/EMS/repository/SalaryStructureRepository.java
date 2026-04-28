package com.app.EMS.repository;

import com.app.EMS.entity.ApprovalStatus;
import com.app.EMS.entity.EmployeeLeave;
import com.app.EMS.entity.Roles;
import com.app.EMS.entity.SalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, Long> {

    Optional<SalaryStructure> findByEmployeeId(String employeeId);

    //    Optional<SalaryStructure> findByUserId(UUID userId);
    boolean existsByEmployeeId(String employeeId);
    List<SalaryStructure> findByStatus(ApprovalStatus status);

    Optional<SalaryStructure> findByEmployeeIdAndStatus(
            String employeeId,
            ApprovalStatus status
    );
    List<SalaryStructure> findByEmployeeIdIn(List<String> employeeIds);
    //    List<SalaryStructure> findByEmployee_Role(Roles role);
    List<SalaryStructure> findByEmployeeIdInAndStatus(
            List<String> employeeIds,
            ApprovalStatus status
    );
//    Optional<SalaryStructure> findByEmployeeEmployeeId(String employeeId);

}
